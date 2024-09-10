import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;

public class MQMessageProcessor {

    public static void main(String[] args) {
        if (args.length < 8) {
            System.out.println(
                    "Uso: java MQMessageProcessor <qMgr> <connName> <channel> <qName> <userId> <password> <action> <messageId>");
            System.out.println("Donde <action> es 'ack' o 'nack'");
            System.out.println("<messageId> es el ID del mensaje en formato hexadecimal");
            System.exit(1);
        }

        String qMgr = args[0];
        String connName = args[1];
        String channel = args[2];
        String qName = args[3];
        String userId = args[4];
        String password = args[5];
        String action = args[6].toLowerCase();
        String messageIdHex = args[7];

        if (!action.equals("ack") && !action.equals("nack")) {
            System.out.println("La acción debe ser 'ack' o 'nack'");
            System.exit(1);
        }

        MQQueueManager qMgrObj = null;
        MQQueue queue = null;

        try {
            // Configurar la conexión
            String[] connNameParts = connName.split("\\(|\\)");
            String hostname = connNameParts[0];
            int port = Integer.parseInt(connNameParts[1]);

            MQEnvironment.hostname = hostname;
            MQEnvironment.port = port;
            MQEnvironment.channel = channel;
            MQEnvironment.userID = userId;
            MQEnvironment.password = password;

            // Conectar al gestor de colas
            qMgrObj = new MQQueueManager(qMgr);

            // Abrir la cola
            int openOptions = CMQC.MQOO_INPUT_SHARED | CMQC.MQOO_BROWSE;
            queue = qMgrObj.accessQueue(qName, openOptions);

            // Procesar el mensaje
            processMessage(queue, qMgrObj, action, messageIdHex);

        } catch (MQException e) {
            System.out.println("Error MQ: " + e.getMessage() + " Código de razón: " + e.reasonCode);
        } catch (Exception e) {
            System.out.println("Error general: " + e.getMessage());
        } finally {
            try {
                if (queue != null)
                    queue.close();
                if (qMgrObj != null)
                    qMgrObj.disconnect();
            } catch (MQException e) {
                System.out.println("Error al cerrar recursos: " + e.getMessage());
            }
        }
    }

    private static void processMessage(MQQueue queue, MQQueueManager qMgrObj, String action, String messageIdHex)
            throws MQException {
        byte[] messageId = hexStringToByteArray(messageIdHex);

        MQMessage message = new MQMessage();
        message.messageId = messageId;

        MQGetMessageOptions gmo = new MQGetMessageOptions();
        gmo.options = CMQC.MQGMO_WAIT | CMQC.MQGMO_SYNCPOINT;
        gmo.matchOptions = CMQC.MQMO_MATCH_MSG_ID;

        gmo.waitInterval = 5000; // Esperar 5 segundos por mensaje

        try {

            queue.get(message, gmo);
            if (action.equals("ack")) {
                qMgrObj.commit();
                System.out.println(messageIdHex);
            } else {
                qMgrObj.backout();
                System.out.println( messageIdHex);
            }

        } catch (MQException e) {
            if (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
                System.out.println("Mensaje no encontrado en la cola: " + messageIdHex);
            } else {
                throw e;
            }
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}