# jasj-ibm-mq

A Node.js module for interacting with IBM MQ, providing a simple interface for sending and receiving messages, as well as managing message acknowledgments. This package includes the necessary IBM MQ Java client library and Java files for ease of use.

## Installation

Install the package using npm:

```bash
npm install jasj-ibm-mq
```

## Prerequisites

This module requires Java to be installed and configured in your PATH. The IBM MQ Java client library (com.ibm.mq.allclient.jar) and necessary Java files are included in this package.

## Usage

Here's a basic example of how to use the `jasj-ibm-mq` module:

```javascript
const IBMMQ = require('jasj-ibm-mq');

const mq = new IBMMQ({
    manager: "YOUR_QUEUE_MANAGER",
    connName: "YOUR_CONNECTION_NAME(PORT)",
    channel: "YOUR_CHANNEL",
    userId: "YOUR_USER_ID",
    password: "YOUR_PASSWORD",
    queue: "YOUR_QUEUE_NAME"
});

// Listen for messages
mq.listen((messageId, content) => {
    console.log(`Received message: ${messageId} - ${content}`);
    
    // Acknowledge the message
    mq.ack(messageId).then(() => {
        console.log(`Message ${messageId} acknowledged`);
    }).catch(err => {
        console.error(`Error acknowledging message ${messageId}:`, err);
    });
});

// Send a message
mq.send("Hello World").then(() => {
    console.log("Message sent successfully");
}).catch(err => {
    console.error("Error sending message:", err);
});
```

## API

[... rest of the API documentation ...]

## Included Files

This package includes the following files to simplify setup:

- `com.ibm.mq.allclient.jar`: The IBM MQ Java client library.
- `MQQueueSender.java`: Java file for sending messages.
- `MQQueueListener.java`: Java file for listening to messages.
- `MQMessageProcessor.java`: Java file for processing messages.

These files are automatically used by the module and do not require any additional setup.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Note: The included IBM MQ Java client library (com.ibm.mq.allclient.jar) is subject to IBM's license terms. Please ensure you comply with IBM's licensing requirements when using this package.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

If you encounter any problems or have any questions, please open an issue in the GitHub repository.

## Author

Juan Andres Segreda Johanning (andres@segreda.com)