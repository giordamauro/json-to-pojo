json-to-pojo
============

Creates Java code classes from Json


For example:

JsonToPojo.fromFile("json.txt", "com.mgiorda.test.ResultClass");

json.txt :
{
    "configurationVersion": {
        "majorVersion": 4, 
        "minorVersion": 0
    }, 
    "contextInfo": "Revision 1 of application weatherTest, in organization apiProvider", 
    "createdAt": 1343182801400, 
    "createdBy": "admin@apigee.com", 
    "lastModifiedAt": 1343182801400, 
    "lastModifiedBy": "admin@apigee.com", 
    "name": "myAPI", 
    "policies": [{
        "majorVersion": 4, 
        "minorVersion": 0
    }], 
    "proxyEndpoints": [{
        "majorVersion": 4, 
        "minorVersion": 0
    }], 
    "resources": [], 
    "revision": "1", 
    "targetEndpoints": [], 
    "targetServers": [], 
    "type": "Application",
    "boolField": false,
    "doubleField": 134.3182801400
}
