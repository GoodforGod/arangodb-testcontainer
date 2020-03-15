package io.testcontainers.arangodb.cluster;

import io.testcontainers.arangodb.containers.ArangoContainer;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 * ArangoDB Cluster TestContainer implementation.
 *
 * @author Anton Kurako (GoodforGod)
 * @see ArangoContainer
 * @since 15.3.2020
 */
public class ArangoClusterContainer extends ArangoContainer {

    public enum NodeType {

        AGENCY_LEADER("agency"),
        AGENCY("agency"),
        DBSERVER("dbserver"),
        COORDINATOR("coordinator");

        private final String alias;

        NodeType(String alias) {
            this.alias = alias;
        }

        public String getAlias() {
            return alias;
        }

        public String getAlias(int number) {
            return alias + number;
        }
    }

    private String endpoint;
    private NodeType type;

    protected ArangoClusterContainer() {
        super();
    }

    protected ArangoClusterContainer(String version) {
        super(version);
    }

    @Override
    protected ArangoContainer withContainerPort(Integer port) {
        return super.withContainerPort(port);
    }

    @Override
    protected Consumer<OutputFrame> getOutputConsumer() {
        return new Slf4jLogConsumer(LoggerFactory.getLogger(getClass().getName() + " (" + type + ")"));
    }

    protected ArangoClusterContainer withAgencyEndpoints(Collection<String> agenciesEndpoints) {
        final String prefix = (NodeType.AGENCY.equals(type) || NodeType.AGENCY_LEADER.equals(type))
                ? "--agency.endpoint"
                : "--cluster.agency-endpoint";

        final StringJoiner cmd = new StringJoiner(" ");
        for (String commandPart : getCommandParts())
            cmd.add(commandPart);

        agenciesEndpoints.forEach(endpoint -> cmd.add(prefix).add(endpoint));
        return (ArangoClusterContainer) this.withCommand(cmd.toString());
    }

    public String getEndpoint() {
        return endpoint;
    }

    public NodeType getType() {
        return type;
    }

    protected static ArangoClusterContainer agency(String alias, int port, String version, int totalAgencyNodes,
                                                   boolean leader) {
        final StringJoiner cmd = new StringJoiner(" ");
        final String endpoint = "tcp://" + alias + ":" + port;
        cmd.add("arangod")
                .add("--server.authentication=false")
                .add("--server.endpoint").add("tcp://0.0.0.0:" + port)
                .add("--agency.my-address").add(endpoint)
                .add("--agency.activate true")
                .add("--agency.size").add(String.valueOf(totalAgencyNodes))
                .add("--agency.supervision true")
                .add("--database.directory").add(alias);

        final ArangoClusterContainer container = build(version, cmd.toString(), alias, port);
        container.type = (leader) ? NodeType.AGENCY_LEADER : NodeType.AGENCY;
        container.endpoint = endpoint;
        return container;
    }

    protected static ArangoClusterContainer dbserver(String alias, int port, String version) {
        final StringJoiner cmd = new StringJoiner(" ");
        final String endpoint = "tcp://" + alias + ":" + port;
        cmd.add("arangod")
                .add("--server.authentication=false")
                .add("--server.endpoint").add("tcp://0.0.0.0:" + port)
                .add("--cluster.my-address").add(endpoint)
//                .add("--cluster.my-local-info").add(alias)
                .add("--cluster.my-role DBSERVER")
                .add("--database.directory").add(alias);

        final ArangoClusterContainer container = build(version, cmd.toString(), alias, port);
        container.endpoint = endpoint;
        container.type = NodeType.DBSERVER;
        return container;
    }

    protected static ArangoClusterContainer coordinator(String alias, int port, String version) {
        final StringJoiner cmd = new StringJoiner(" ");
        final String endpoint = "tcp://" + alias + ":" + port;
        cmd.add("arangod")
                .add("--server.authentication=false")
                .add("--server.endpoint").add("tcp://0.0.0.0:" + port)
                .add("--cluster.my-address").add(endpoint)
//                .add("--cluster.my-local-info").add(alias)
                .add("--cluster.my-role COORDINATOR")
                .add("--database.directory").add(alias);

        final ArangoClusterContainer container = build(version, cmd.toString(), alias, port);
        container.endpoint = endpoint;
        container.type = NodeType.COORDINATOR;
        return container;
    }

    private static ArangoClusterContainer build(String version, String cmd, String networkAliasName, int port) {
        return (ArangoClusterContainer) new ArangoClusterContainer(version)
                .withContainerPort(port).withPort(port)
                .withoutAuth()
                .withExposedPorts(port)
                .withNetworkAliases(networkAliasName)
                .withCommand(cmd);
    }
}