package org.krisbox.MQTT;

import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ResultSet;
import com.mysql.jdbc.ResultSetMetaData;
import com.mysql.jdbc.Statement;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static org.kohsuke.args4j.ExampleMode.ALL;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

public class SQL2MQTT {
	private MqttClient         mqttClient;
	private MqttConnectOptions mqttConnectionOpts;
	
	@Option(name="-mysqlHostname",  usage="Set's the MySQL Hostname.")
	private String mysqlHostname;
	
	@Option(name="-mysqlDatabase",  usage="Set's the MySQL Database.")
	private String mysqlDatabase;
	
	@Option(name="-mysqlTable",     usage="Set's the MySQL Table.")
	private String mysqlTable;
	
	@Option(name="-mysqlUser",      usage="Set's the MySQL Username.")
	private String mysqlUser;
	
	@Option(name="-mysqlPassword",  usage="Set's the MySQL Password.")
	private String mysqlPassword;
	
	@Option(name="-mysqlPort",      usage="Set's the MySQL Port.")
	private String mysqlPort;
	
	@Option(name="-wait",           usage="Set's the wait time between publishing MQTT Messages in milliseconds")
	private String wait;
	
	@Option(name="-brokerHostname", usage="Set's the MQTT Broker Hostname")
	private String brokerHostname;
	
	@Option(name="-brokerPort",     usage="Set's the MQTT Broker Port")
	private String brokerPort;
	
	@Option(name="-v",              usage="Show messages as they are published")
	private boolean v;
	
	@Option(name="-h",              usage="Shows the help menu")
	private boolean h;
	
	 @Argument
	 private List<String> arguments = new ArrayList<String>();
	 
	
	final String clientID = "MQTTDemo";
	final int    qos      = 0;
	MemoryPersistence persistence = new MemoryPersistence();
	
	public void start() {
		try {
			String broker       = new String("tcp://" + this.brokerHostname + ":" + this.brokerPort);
			
			mqttClient          = new MqttClient(broker, clientID, persistence);
			mqttConnectionOpts  = new MqttConnectOptions();
			
			mqttConnectionOpts.setCleanSession(true);
			System.out.print("Connecting to broker: " + broker + "....");
			mqttClient.connect(mqttConnectionOpts);
			System.out.println("[DONE]");
			
			System.out.print("Running....");
			connect();
			System.out.println("[DONE]");
			
			mqttClient.disconnect();
			System.out.println("Disconnected!");
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	private void publishDataPoint(String topic, String data) {
		try {
			MqttMessage message = new MqttMessage(data.getBytes());
			message.setQos(this.qos);
			this.mqttClient.publish("/" + topic, message);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void connect() {
		try {
			// MySQL Database Connection
			String myDriver = "com.mysql.jdbc.Driver";
			String myURL    = "jdbc:mysql://" + this.mysqlHostname + ":" + this.mysqlPort + "/" + this.mysqlDatabase;
			Class.forName(myDriver);
			Connection conn = (Connection) DriverManager.getConnection(myURL, this.mysqlUser, this.mysqlPassword);
			
			// SQL Query
			String query = "SELECT * FROM raw_logs";
			
			// Create the Java statement
			Statement st = (Statement) conn.createStatement();
			
			// Execute the query
			ResultSet rs = (ResultSet) st.executeQuery(query);
			
			// Get the column names
			ResultSetMetaData meta = (ResultSetMetaData) rs.getMetaData();
			
			// Iterate through the Java ResultSet
			while(rs.next()) {
				for(int i=1; i<=meta.getColumnCount(); i++) {
					// Get the current value
					String currentValue = rs.getObject(meta.getColumnName(i)).toString();
					
					// Publish the message
					this.publishDataPoint(meta.getColumnName(i), currentValue);
					
					if(this.v == true)
						System.out.println("/" + meta.getColumnName(i) + ": " + currentValue);
				}
				Thread.sleep(Integer.parseInt(this.wait));
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void doMain(String[] args) throws IOException {
		CmdLineParser parser = new CmdLineParser(this);
		
		parser.setUsageWidth(80);
		try {
			parser.parseArgument(args);
			
			if(h == true)
				throw new CmdLineException(parser, "Help menu");
		}catch(Exception ex){
			System.err.println(ex.getMessage());
			System.err.println("java SampleMain [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();
			// print option sample. This is useful some time
			System.err.println(" Example: java SampleMain"+parser.printExample(ALL));
			return;
		}
		
		start();
	}
	
	public static void main(String[] args) throws IOException {
		new SQL2MQTT().doMain(args);
	}
}
