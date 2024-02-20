package cloud.cave.cdt;

import kong.unirest.Empty;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

@Testcontainers
public class CDTWallMessageService {

    public static final int SERVER_PORT = 8080;

    public static final String PATH = "/v1/messages/";

    @Container
    public GenericContainer messageService =
            new GenericContainer(DockerImageName.parse("hub.baerbak.com/juliett/message-service:latest"))
                    .withExposedPorts(SERVER_PORT)
                    .withEnv("ENVIRONMENT","fake");
    private String serverRootUrl;

    @BeforeEach
    public void setup()
    {
        String address = messageService.getHost();
        Integer port = messageService.getMappedPort(SERVER_PORT);
        serverRootUrl = "http://" + address + ":" + port;
    }

    @Test
    public void mustReturn404WhenNoMessagesAreFound() {
        // Given the message service is online and available
        // And there isn't a room at position (0,0,0)

        // When calling the service with a valid position and query parameters
        final HttpResponse<JsonNode> reply = Unirest
                .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();

        // Then the service must return status code 404
        assertThat("Must return status code 404", reply.getStatus(), is(404));
    }

    @Test
    public void mustReturn200AndValidMessagesWhenMessagesAreRetrieved() throws ParseException {
        // Given the message service is online and available
        // And there are messages on a wall at position (0,0,0)
        final MessageRecord messageRecord = new MessageRecord("Message 1", "andersId", "Anders");
        Unirest.post(serverRootUrl + PATH + "(0,0,0)")
                .body(messageRecord)
                .contentType("application/json")
                .asJson();

        // When calling the service with a valid position and query parameters
        final HttpResponse<JsonNode> reply = Unirest
            .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
            .accept("application/json")
            .asJson();

        // Then the service must return status code 200
        assertThat("Must return status code 200", reply.getStatus(), is(200));
        // And the service must return a list of messages
        final JSONArray array = reply.getBody().getArray();
        assertThat("Must return a list of messages", array.length(), is(not(0)));
        // And each message must contain an id, a message, a creatorId, a creatorName, and a creatorTimeStampISO8601
        final JSONObject object = array.getJSONObject(0);
        assertThat("Must return id", object.getString("id"), is(not(emptyString())));
        assertThat("Must return message", object.getString("contents"), is(not(emptyString())));
        assertThat("Must return creatorId", object.getString("creatorId"), is(not(emptyString())));
        assertThat("Must return creator name", object.getString("creatorName"), is(not(emptyString())));
        final String creatorTimeStampISO8601 = object.getString("creatorTimeStampISO8601");
        assertThat("Must return creator TimeStamp ISO8601", creatorTimeStampISO8601, is(not(emptyString())));

        // And the creatorTimeStampISO8601 must be a valid ISO8601 date
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        df.parse(creatorTimeStampISO8601);
    }

    @Test
    public void mustReturn400WhenFetchingAndBadPositionSupplied() {
        // Given the message service is online and available

        // When calling the service with a bad position
        final HttpResponse<JsonNode> reply = Unirest
                .get(serverRootUrl + PATH + "bad,id?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();

        // Then the service must return status code 400
        assertThat("Must return status code 400", reply.getStatus(), is(400));
    }

    @Test
    public void mustReturn201OnCreateMessage() {
        // Given the message service is online and available
        // And there are no messages on a wall at position (0,0,0)

        // When posting a message to a wall at position (0,0,0)
        final MessageRecord messageRecord = new MessageRecord("Message 1", "andersId", "Anders");
        final HttpResponse<JsonNode> replyPost = Unirest
                .post(serverRootUrl + PATH + "(0,0,0)")
                .body(messageRecord)
                .contentType("application/json")
                .asJson();

        // Then the service must return status code 201
        assertThat("Must return status code 201", replyPost.getStatus(), is(201));

        // And the newly posted message must be retrievable
        final HttpResponse<JsonNode> replyGet = Unirest
                .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();
        final JSONArray array = replyGet.getBody().getArray();
        assertThat("Must return one message", array.length(), is(1));
        final JSONObject objectGet = array.getJSONObject(0);
        assertThat("Must return the same message that was posted", objectGet.getString("contents"), is(messageRecord.getContents()));
        assertThat("Must return the same creatorId that was posted", objectGet.getString("creatorId"), is(messageRecord.getCreatorId()));
        assertThat("Must return the same creatorName that was posted", objectGet.getString("creatorName"), is(messageRecord.getCreatorName()));
    }

    @Test
    public void mustIgnoreIdAndTimestampOnCreateMessage() {
        // Given the message service is online and available
        // And there are no messages on a wall at position (0,0,0)

        // When posting a message to a wall at position (0,0,0) with an id and a creatorTimeStampISO8601
        final MessageRecord messageRecord = new MessageRecord("Message 1", "andersId", "Anders");
        final String userSetId = "user-set-id";
        final String userSetTimestamp = "user-set-timestamp";
        messageRecord.setId(userSetId);
        messageRecord.setCreatorTimeStampISO8601(userSetTimestamp);
        final HttpResponse<JsonNode> replyPost = Unirest
                .post(serverRootUrl + PATH + "(0,0,0)")
                .body(messageRecord)
                .contentType("application/json")
                .asJson();

        // Then the service must return status code 201
        assertThat("Must return status code 201", replyPost.getStatus(), is(201));

        // And the newly posted message should not contain the id and creatorTimeStampISO8601 that was posted
        final HttpResponse<JsonNode> replyGet = Unirest
                .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();
        final JSONObject objectGet = replyGet.getBody().getArray().getJSONObject(0);
        assertThat("Must ignore the id that was posted", objectGet.getString("id"), is(not(userSetId)));
        assertThat("Must ignore the creatorTimeStampISO8601 that was posted", objectGet.getString("creatorTimeStampISO8601"), is(not(userSetTimestamp)));
    }

    @Test
    public void mustReturn201OnSuccessfulMessageUpdate() {
        // Given the message service is online and available
        // And there is a message on a wall at position (0,0,0)
        final MessageRecord oldMessageRecord = new MessageRecord("lesage 1", "andersId", "Anders");
        Unirest.post(serverRootUrl + PATH + "(0,0,0)")
                .body(oldMessageRecord)
                .contentType("application/json")
                .asEmpty();

        final HttpResponse<JsonNode> oldReplyGet = Unirest
                .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();
        final JSONArray oldArray = oldReplyGet.getBody().getArray();
        final String messageId = oldArray.getJSONObject(0).getString("id");
        final String creatorTimeStampISO8601 = oldArray.getJSONObject(0).getString("creatorTimeStampISO8601");

        // When updating the message on the wall at position (0,0,0)
        final MessageRecord newMessageRecord = new MessageRecord("Message 1", "andersId", "Anders");
        final HttpResponse<Empty> replyPut = Unirest
                .put(serverRootUrl + PATH + "(0,0,0)/" + messageId)
                .body(newMessageRecord)
                .contentType("application/json")
                .asEmpty();

        // Then the service must return status code 201
        assertThat("Must return status code 201", replyPut.getStatus(), is(201));

        // And the updated message must be retrievable
        final HttpResponse<JsonNode> newReplyGet = Unirest
                .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();
        final JSONArray newArray = newReplyGet.getBody().getArray();
        assertThat("Must return one message", newArray.length(), is(1));

        // And the updated message must use the same ID as the original message
        final JSONObject objectGet = newArray.getJSONObject(0);
        assertThat("Must return the same id that was posted", objectGet.getString("id"), is(messageId));

        // And the updated message must contain the new message
        assertThat("Must return the updated message", objectGet.getString("contents"), is(newMessageRecord.getContents()));
        assertThat("Must return the same creatorId that was posted", objectGet.getString("creatorId"), is(newMessageRecord.getCreatorId()));
        assertThat("Must return the same creatorName that was posted", objectGet.getString("creatorName"), is(newMessageRecord.getCreatorName()));

        // And the updated message must use the same creatorTimeStampISO8601 as the original message
        assertThat("Must return a new creator TimeStamp ISO8601", objectGet.getString("creatorTimeStampISO8601"), is(creatorTimeStampISO8601));
    }

    @Test
    public void mustReturn400WhenCreatingAndBadPositionSupplied() {
        // Given the message service is online and available

        // When calling the service with a bad position
        final HttpResponse<JsonNode> reply = Unirest
                .post(serverRootUrl + PATH + "bad,id")
                .body(new MessageRecord("Message 1", "andersId", "Anders"))
                .contentType("application/json")
                .asJson();

        // Then the service must return status code 400
        assertThat("Must return status code 400", reply.getStatus(), is(400));
    }

    @Test
    public void mustReturn400WhenUpdatingAndBadPositionSupplied() {
        // Given the message service is online and available

        // When calling the service with a bad position
        final HttpResponse<JsonNode> reply = Unirest
                .get(serverRootUrl + PATH + "bad,id?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();

        // Then the service must return status code 400
        assertThat("Must return status code 400", reply.getStatus(), is(400));
    }

    @Test
    public void mustReturn400WhenUpdatingAndBadMessageIdSupplied() {
        // Given the message service is online and available
        // And there is a message on a wall at position (0,0,0)
        final MessageRecord newMessageRecord = new MessageRecord("Message 1", "andersId", "Anders");
        Unirest.post(serverRootUrl + PATH + "(0,0,0)")
                .body(newMessageRecord)
                .contentType("application/json")
                .asEmpty();

        // When updating the message on the wall at position (0,0,0) with a bad message id
        final HttpResponse<Empty> replyPut = Unirest
                .put(serverRootUrl + PATH + "(0,0,0)/bad-message-id")
                .body(newMessageRecord)
                .contentType("application/json")
                .asEmpty();

        // Then the service must return status code 400
        assertThat("Must return status code 400", replyPut.getStatus(), is(400));
    }

    @Test
    public void mustReturn403WhenUpdatingUserDoesNotMatchCreatorId() {
        // Given the message service is online and available
        // And there is a message on a wall at position (0,0,0) created by andersId
        final MessageRecord oldMessageRecord = new MessageRecord("Message 1", "andersId", "Anders");
        Unirest.post(serverRootUrl + PATH + "(0,0,0)")
                .body(oldMessageRecord)
                .contentType("application/json")
                .asEmpty();

        final HttpResponse<JsonNode> oldReplyGet = Unirest
                .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();
        final JSONArray oldArray = oldReplyGet.getBody().getArray();
        final String messageId = oldArray.getJSONObject(0).getString("id");

        // When updating the message on the wall at position (0,0,0) with a different creatorId
        final MessageRecord newMessageRecord = new MessageRecord("Message 1", "notAndersId", "Not Anders");
        final HttpResponse<Empty> replyPut = Unirest
                .put(serverRootUrl + PATH + "(0,0,0)/" + messageId)
                .body(newMessageRecord)
                .contentType("application/json")
                .asEmpty();

        // Then the service must return status code 403
        assertThat("Must return status code 403", replyPut.getStatus(), is(403));
    }

    @Test
    public void mustReturn404WhenPositionDoesNotExist() {
        // Given the message service is online and available
        // And there is no room at position (0,0,0)

        // When calling the service with a position that does not exist
        final HttpResponse<JsonNode> reply = Unirest
                .get(serverRootUrl + PATH + "(0,0,0)?startIndex=0&pageSize=8")
                .accept("application/json")
                .asJson();

        // Then the service must return status code 404
        assertThat("Must return status code 404", reply.getStatus(), is(404));
    }
}
