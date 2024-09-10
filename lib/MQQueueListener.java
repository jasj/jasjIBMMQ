import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQGetMessageOptions;

public class MQQueueListener {

    public static void main(String[] args) {
        // Verificar que se proporcionaron todos los argumentos necesarios
        if (args.length < 6) {
            System.out.println("Uso: java MQQueueListener <qMgr> <connName> <channel> <qName> <userId> <password>");
            System.exit(1);
        }

        // Configuración de la conexión desde argumentos
        String qMgr = args[0];
        String connName = args[1];
        String channel = args[2];
        String qName = args[3];
        String userId = args[4];
        String password = args[5];

        System.out.println("Iniciando conexión a MQ...");

        MQQueueManager qMgrObj = null;
        MQQueue queue = null;

        try {
            // Establecer la conexión
            String[] connNameParts = connName.split("\\(|\\)");
            String hostname = connNameParts[0];
            int port = Integer.parseInt(connNameParts[1]);

            com.ibm.mq.MQEnvironment.hostname = hostname;
            com.ibm.mq.MQEnvironment.port = port;
            com.ibm.mq.MQEnvironment.channel = channel;
            com.ibm.mq.MQEnvironment.userID = userId;
            com.ibm.mq.MQEnvironment.password = password;

            // Conectar al gestor de colas
            qMgrObj = new MQQueueManager(qMgr);
            System.out.println("Conexión a MQ establecida");

            // Abrir la cola para lectura
            int openOptions = CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_BROWSE;
            queue = qMgrObj.accessQueue(qName, openOptions);
            System.out.println("Cola abierta para lectura");

            // Configurar opciones de lectura
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = CMQC.MQGMO_WAIT | CMQC.MQGMO_BROWSE_FIRST;
            gmo.waitInterval = 5000; // Esperar 5 segundos por mensaje

            System.out.println("Escuchando mensajes en la cola " + qName + "...");
            
            boolean continueListening = true;
            while (continueListening) {
                try {
                    MQMessage receivedMessage = new MQMessage();
                    queue.get(receivedMessage, gmo);
                    
                    // Procesar el mensaje
                    String mId  = toHexString(receivedMessage.messageId);
                    String msgText = receivedMessage.readStringOfByteLength(receivedMessage.getMessageLength());
                    System.out.println("Mensaje recibido: ("+mId+")" + msgText);

                    // Si el mensaje es "STOP", detenemos la escucha
                    if ("STOP".equalsIgnoreCase(msgText.trim())) {
                        continueListening = false;
                        System.out.println("Comando de detención recibido. Finalizando...");
                    }

                    // Prepararse para el siguiente mensaje
                    gmo.options = CMQC.MQGMO_WAIT | CMQC.MQGMO_BROWSE_NEXT;
                } catch (MQException e) {
                    if (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
                        // No hay mensajes, continuamos esperando
                    } else {
                        throw e; // Re-lanzar otras excepciones
                    }
                }
            }

        } catch (MQException mqe) {
            System.out.println("Error MQ: " + mqe.getMessage() + " Código de razón: " + mqe.reasonCode);
        } catch (java.io.IOException ioe) {
            System.out.println("Error IO: " + ioe.getMessage());
        } catch (Exception e) {
            System.out.println("Error general: " + e.getMessage());
        } finally {
            try {
                if (queue != null) {
                    queue.close();
                    System.out.println("Cola cerrada");
                }
                if (qMgrObj != null) {
                    qMgrObj.disconnect();
                    System.out.println("Desconexión completada");
                }
            } catch (MQException e) {
                System.out.println("Error al cerrar recursos: " + e.getMessage());
            }


        }
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}