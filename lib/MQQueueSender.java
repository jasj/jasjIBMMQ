import javax.print.DocFlavor.STRING;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQMessage;

public class MQQueueSender {

    public static void main(String[] args) {
        // Verificar que se proporcionaron todos los argumentos necesarios
        if (args.length < 6) {
            System.out.println("Uso: java MQQueueSender <qMgr> <connName> <channel> <qName> <userId> <password>");
            System.exit(1);
        }

        // Configuración de la conexión desde argumentos
        String qMgr = args[0];
        String connName = args[1];
        String channel = args[2];
        String qName = args[3];
        String userId = args[4];
        String password = args[5];
        String msg = args[6];

        // Configuración de la conexión
        MQQueueManager qMgrObj = null;
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

            // Abrir la cola
            MQQueue queue = qMgrObj.accessQueue(qName, CMQC.MQOO_OUTPUT);

            // Preparar y poner un mensaje en la cola
            MQMessage message = new MQMessage();
            message.writeString(msg);
            MQPutMessageOptions pmo = new MQPutMessageOptions();
            queue.put(message, pmo);

            // Cerrar la cola y la conexión
            queue.close();
            qMgrObj.disconnect();

        } catch (MQException mqe) {
            System.out.println("Error MQ: " + mqe.getMessage());
        } catch (java.io.IOException ioe) {
            System.out.println("Error IO: " + ioe.getMessage());
        } catch (Exception e) {
            System.out.println("Error general: " + e.getMessage());
        } finally {
            if (qMgrObj != null) {
                try {
                    qMgrObj.disconnect();
                } catch (MQException e) {
                    System.out.println("Error al desconectar: " + e.getMessage());
                }
            }
        }
    }
}