package consumidorapp;

import com.fazecast.jSerialComm.SerialPort;
import com.rabbitmq.client.*;

public class Consumidor {
    private final static String QUEUE_NAME = "cola_mensajes_consumidor";
    private final static String QUEUE_PRODUCER = "cola_mensajes_productor";

    private SerialPort serialPort;
    private static final String PUERTO_BLUETOOTH = "COMx";  // Cambia "COMx" por el puerto de tu dispositivo Bluetooth

    public static void main(String[] argv) throws Exception {
        Consumidor consumidor = new Consumidor();
        consumidor.iniciarRabbitMQ();
        consumidor.conectarBluetooth();
    }

    // Conectar a Bluetooth
    public boolean conectarBluetooth() {
        serialPort = SerialPort.getCommPort(PUERTO_BLUETOOTH);
        serialPort.setBaudRate(9600);  // BaudRate debe coincidir con el de Arduino (9600)

        if (serialPort.openPort()) {
            System.out.println("Conexión Bluetooth establecida.");
            return true;
        } else {
            System.out.println("No se pudo conectar al puerto Bluetooth.");
            return false;
        }
    }

    // Método para enviar comandos al Arduino via Bluetooth
    public void enviarComandoBluetooth(String comando) {
        try {
            byte[] mensaje = comando.getBytes();
            serialPort.writeBytes(mensaje, mensaje.length);
            System.out.println("Comando enviado: " + comando);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Cerrar conexión Bluetooth
    public void cerrarConexionBluetooth() {
        if (serialPort.isOpen()) {
            serialPort.closePort();
            System.out.println("Conexión Bluetooth cerrada.");
        }
    }

    // Iniciar RabbitMQ
    public void iniciarRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672); // Puerto normal
        factory.setUsername("started");
        factory.setPassword("started");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.queueDeclare(QUEUE_PRODUCER, false, false, false, null);

            System.out.println("Consumidor listo para recibir mensajes...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("Recibido: " + message);

                // Enviar comando a Arduino según el mensaje recibido
                if (message.matches("Arriba|Abajo|Izquierda|Derecha")) {
                    enviarComandoBluetooth(message);
                } else {
                    System.out.println("Comando inválido recibido: " + message);
                }
            };

            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        }
    }
}
