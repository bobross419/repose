package features.filters.rackspaceAuthIdentity

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/*
 * rackspace auth identity test include test for both versions v2.0 and v1.1
 */

class RackspaceAuthIdentityTest extends ReposeValveTest {

    static Map contentXml = ["content-type": "application/xml"]
    static Map contentJSON = ["content-type": "application/json"]

    static String xmlPasswordCred = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns="http://docs.openstack.org/identity/api/v2.0">
  <passwordCredentials username="demoauthor" password="theUsersPassword" tenantId="1100111"/>
</auth>"""


    static String xmlPasswordCredEmptyKey = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns="http://docs.openstack.org/identity/api/v2.0">
  <passwordCredentials username="demoauthor" password="" tenantId="1100111"/>
</auth>"""

    static String invalidData = "Invalid data"
    static String xmlOverLimit = "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><credential xsi:type=\"PasswordCredentialsRequiredUsername\" username=\"012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\" password=\"testpwd\" /></auth>"

    static String jsonPasswordCred = """
{
    "auth":{
        "passwordCredentials":{
            "username":"demoauthor",
            "password":"theUsersPassword"
        },
        "tenantId": "12345678"
    }
}
"""

    def static String jsonApiKeyCred = """{
    "auth": {
        "RAX-KSKEY:apiKeyCredentials": {
            "username": "demoauthor",
            "apiKey": "aaaaa-bbbbb-ccccc-12345678"
        },
        "tenantId": "1100111"
    }
}
"""
    //v1.1
    static String jsonKeyCred11 = "{ \"credentials\": { \"username\": \"test-user\", \"key\": \"testpwd\"}}"
    static String jsonKeyCredEmptyKey11 = "{ \"credentials\": { \"username\": \"test-user\", \"key\": \"\"}}"
    static String xmlKeyCred11 = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" key=\"testpwd\" />"
    static String xmlKeyCredEmptyKey11 = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" key=\"\" />"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/rackspaceAuthIdentity", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }


    @Unroll("When request contains identity 2.0 in content #testName Expected user is #expectedUser")
    def "when identifying requests by header"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-user").equals(expectedUser + ";q=0.8")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-groups").equals("2_0 Group;q=0.8")

        where:
        requestBody             | contentType | expectedUser | testName
        xmlPasswordCred         | contentXml  | "demoauthor" | "xmlPasswordCred"
        xmlPasswordCredEmptyKey | contentXml  | "demoauthor" | "xmlPasswordCredEmptyKey"
        jsonPasswordCred        | contentJSON | "demoauthor" | "jsonPasswordKey"
        jsonApiKeyCred          | contentJSON | "demoauthor" | "jsonApiKey"
    }


    @Unroll("When bad requests pass through repose #testName")
    def "when attempting to identity user by content and passed bad content"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType, method:"POST"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will not send x-pp-user"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 0

        and: "Repose will not send x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 0

        where:
        requestBody  | contentType | testName
        invalidData  | contentXml  | "invalidData"
        xmlOverLimit | contentXml  | "xmlOverLimit"
    }
    // Joe Savak - we don't need to do other v1.1 internal contracts
    @Unroll("When request contains identity 1.1 in content #testName Expected user is #expectedUser")
    def "when using identity1.1 identifying requests by header"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-user").equals(expectedUser + ";q=0.75")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-groups").equals("1_1 Group;q=0.75")

        where:
        requestBody           | contentType | expectedUser | testName
        xmlKeyCred11          | contentXml  | "test-user"  | "xmlKeyCred11"
        xmlKeyCredEmptyKey11  | contentXml  | "test-user"  | "xmlKeyCredEmptyKey11"
        jsonKeyCred11         | contentJSON | "test-user"  | "jsonKeyCred11"
        jsonKeyCredEmptyKey11 | contentJSON | "test-user"  | "jsonKeyCredEmptyKey11"
    }

    @Unroll("Does not affect #method requests")
    def "Does not affect non-post requests"() {
        when: "Request is a #method"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, method: method])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will not add any headers"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 0
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 0

        and: "The result will be passed through"
        messageChain.receivedResponse.code == "200"

        where:
        method   | _
        "GET"    | _
        "DELETE" | _
        "PUT"    | _
    }

    def "Does not affect random post requests"() {
        when:
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, method: 'POST'])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will not add any headers"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 0
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 0

        and: "The result will be passed through"
        messageChain.receivedResponse.code == "200"
    }
}