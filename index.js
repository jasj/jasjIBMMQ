const { exec, spawn } = require('child_process');
const path = require('path');
const fs = require('fs').promises;
const os = require('os');
const crypto = require('crypto');

class ibmJASJMQ {
    constructor({ manager, connName, channel, userId, password, queue, maxReconnectAttempts }) {
        this.manager = manager;
        this.connName = connName;
        this.channel = channel;
        this.userId = userId;
        this.password = password;
        this.queue = queue;
        this.listenerProcess = null;
        this.isReconnecting = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = maxReconnectAttempts;
        this.reconnectInterval = 5000; // 5 segundos
    }

    async sendFileContent(msg) {
        // Crear un nombre de archivo temporal único
        const tempFileName = path.join(
            os.tmpdir(),
            `mq-msg-${Date.now()}-${crypto.randomBytes(16).toString('hex')}.tmp`);

        try {
            // Escribir el contenido en el archivo temporal
            await fs.writeFile(tempFileName, msg);

            // Ejecutar el comando usando MQQueueFileSender
            const command = `java -cp ${path.join(__dirname, 'lib', 'com.ibm.mq.allclient.jar')} ${path.join(__dirname, 'lib', 'MQQueueFileSender.java')} "${this.manager}" "${this.connName}" "${this.channel}" "${this.queue}" "${this.userId}" "${this.password}" "${tempFileName}"`;

            return new Promise((resolve, reject) => {
                exec(command, async (error, stdout, stderr) => {
                    try {
                        // Intentar eliminar el archivo temporal sin importar el resultado
                        await fs.unlink(tempFileName);
                    } catch (deleteError) {
                        console.error(`Error al eliminar archivo temporal: ${deleteError}`);
                    }

                    if (error) {
                        console.error(`Error al ejecutar el comando: ${error}`);
                        reject(false);
                        return;
                    }
                    if (stderr) {
                        console.error(`Error en la salida estándar: ${stderr}`);
                        reject(false);
                        return;
                    }
                    resolve(true);
                });
            });
        } catch (error) {
            // Si ocurre un error al escribir el archivo, intentar eliminarlo
            try {
                await fs.unlink(tempFileName);
            } catch (deleteError) {
                // Ignorar errores al eliminar si el archivo no existe
            }
            throw error;
        }
    }

    send(msg) {
        if (msg.length > 64000) {
            return this.sendFileContent(msg);
        }
        return new Promise((resolve, reject) => {
            const command = `java -cp ${path.join(__dirname, 'lib', 'com.ibm.mq.allclient.jar')} ${path.join(__dirname, 'lib', 'MQQueueSender.java')} "${this.manager}" "${this.connName}" "${this.channel}" "${this.queue}" "${this.userId}" "${this.password}" '${msg}'`;

            exec(command, (error, stdout, stderr) => {
                if (error) {
                    console.error(`Error al ejecutar el comando: ${error}`);
                    reject(false);
                    return;
                }
                if (stderr) {
                    console.error(`Error en la salida estándar: ${stderr}`);
                    reject(false);
                    return;
                }
                resolve(true);
            });
        });
    }

    listen(handle) {
        const startListener = () => {
            this.listenerProcess = spawn('java', [
                '-cp', path.join(__dirname, 'lib', 'com.ibm.mq.allclient.jar'),
                path.join(__dirname, 'lib', 'MQQueueListener.java'),
                this.manager, this.connName, this.channel, this.queue, this.userId, this.password
            ]);

            this.listenerProcess.stdout.on('data', (data) => {
                const lines = data.toString().split('\n');
                lines.forEach(line => {
                    if (line.startsWith('Mensaje recibido:')) {
                        const match = line.match(/\((.*?)\)(.*)/);
                        if (match) {
                            const messageId = match[1];
                            const messageContent = match[2].trim();
                            handle(messageId, messageContent);
                        }
                    } else if (line.startsWith("Conexión a MQ establecida")) {
                        console.log("go to 0")
                        this.reconnectAttempts = 0
                    }
                });
            });

            this.listenerProcess.stderr.on('data', (data) => {
                console.error(`Error en la escucha: ${data}`);
            });

            this.listenerProcess.on('close', (code) => {
                console.log(`Proceso de escucha finalizado con código ${code}`);
                if (!this.isReconnecting) {
                    this.reconnect(startListener);
                }
            });

            this.listenerProcess.on('error', (err) => {
                console.error(`Error en el proceso de escucha: ${err}`);
                if (!this.isReconnecting) {
                    this.reconnect(startListener);
                }
            });
        };

        startListener();
        return this;
    }

    reconnect(startListener) {
        if (this.isReconnecting) return;
        this.isReconnecting = true;
        this.reconnectAttempts++;

        console.log(`Intento de reconexión ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);

        if (this.reconnectAttempts > this.maxReconnectAttempts) {
            console.error("Número máximo de intentos de reconexión alcanzado. Deteniendo el proceso.");
            this.isReconnecting = false;
            return;
        }

        setTimeout(() => {
            console.log("Intentando reconectar...");
            this.isReconnecting = false;
            startListener();
        }, this.reconnectInterval);
    }

    stop() {
        if (this.listenerProcess) {
            console.log("Deteniendo el proceso de escucha...");
            this.listenerProcess.kill();
            this.listenerProcess = null;
        }
    }

    ack(messageId) {
        return this._processMessage(this.queue, messageId, 'ack');
    }

    nack(messageId) {
        return this._processMessage(this.queue, messageId, 'nack');
    }

    _processMessage(queue, messageId, action) {
        return new Promise((resolve, reject) => {
            const command = `java -cp ${path.join(__dirname, "lib", 'com.ibm.mq.allclient.jar')} ${path.join(__dirname, 'lib', 'MQMessageProcessor.java')} "${this.manager}" "${this.connName}" "${this.channel}" "${queue}" "${this.userId}" "${this.password}" ${action} ${messageId}`;

            exec(command, (error, stdout, stderr) => {
                if (error) {
                    console.error(`Error al ejecutar ${action.toUpperCase()}: ${error}`);
                    reject(false);
                    return;
                }
                if (stderr) {
                    console.error(`Error en la salida estándar de ${action.toUpperCase()}: ${stderr}`);
                    reject(false);
                    return;
                }
                resolve(stdout);
            });
        });
    }
}

module.exports = ibmJASJMQ;
