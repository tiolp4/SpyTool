const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendStegoNotification = functions.firestore
    .document("stego_messages/{messageId}")
    .onCreate(async (snap, context) => {

        const data = snap.data();
        if (!data) return null;

        const receiverId = data.receiverId;
        const senderId = data.senderId;

        if (!receiverId || !senderId) return null;

        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(receiverId)
            .get();

        if (!receiverDoc.exists) return null;

        const fcmToken = receiverDoc.data().fcmToken;
        if (!fcmToken) return null;

        const senderDoc = await admin.firestore()
            .collection("users")
            .doc(senderId)
            .get();

        const senderName =
            senderDoc.exists
                ? senderDoc.data().displayName || senderDoc.data().email || "Агент"
                : "Агент";

        const message = {
            token: fcmToken,
            notification: {
                title: "Новое стего-сообщение",
                body: `Сообщение от ${senderName}`
            },
            android: {
                priority: "high",
                notification: {
                    sound: "default"
                }
            }
        };

        return admin.messaging().send(message);
    });
