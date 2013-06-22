package example.sharding;

import static example.sharding.Index.genPartition;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.mock.MockInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.hadoop.io.Text;

public class ShardByFirstNameDriver {

    public class Person {
        public String firstName;
        public String lastName;
        public String age;
    }
    
    private Text genPartition(int partition) {
        return new Text(String.format("%08d", Math.abs(partition)));
    }

    private void index(int numPartitions, Text docId, String doc, String splitRegex, BatchWriter bw) throws MutationsRejectedException {
        String[] tokens = doc.split(splitRegex);
        Text partition = genPartition(doc.hashCode() % numPartitions);
        Mutation m = new Mutation(partition);
        HashSet<String> tokensSeen = new HashSet<String>();

        for (String token : tokens) {
            token = token.toLowerCase();

            if (!tokensSeen.contains(token)) {
                tokensSeen.add(token);
                m.put(new Text(token), docId, new Value(new byte[0]));
            }
        }

        if (m.size() > 0) {
            bw.addMutation(m);
        }
    }

    private void process() throws AccumuloSecurityException, AccumuloException, TableExistsException, TableNotFoundException {
        String instanceName = "development";
        String user = "root";
        String password = "";
        String tableName = "TABLEA";
        String splitRegex = "\\W+";
        int partitionCount = 10;

        Instance mock = new MockInstance(instanceName);
        Connector connector = mock.getConnector(user, new PasswordToken(password));
        connector.tableOperations().create(tableName);

        // Use the default values for configuring a BatchWriter.
        BatchWriterConfig config = new BatchWriterConfig();

        BatchWriter bw = connector.createBatchWriter(tableName, config);
        index(partitionCount, new Text("D1"), "Now is the time for all men to come to the aid of thier country.", splitRegex, bw);
        index(partitionCount, new Text("D1"), "The quick brown fox jumps over the lazy dog.", splitRegex, bw);
        bw.close();

        Scanner scanner = connector.createScanner(tableName, new Authorizations());
        Iterator<Map.Entry<Key, Value>> iterator = scanner.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Value> entry = iterator.next();
            Key key = entry.getKey();
            Value value = entry.getValue();
            System.out.println(key + " -> " + value);
        }
    }

    public static void main(String[] args) throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
        ShardByFirstNameDriver driver = new ShardByFirstNameDriver();
        driver.process();
    }
}
