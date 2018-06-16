HEDEX Implementation
--------------------

Hedex installs into Sakai and starts gathering data on user logins, assignment
submissions, and course visits. This data is made available on a set of RESTful
endpoints, authenticated with a particular, configurable user.

Installation
------------

Copy this code into your Sakai installation, alongside the other Sakai modules
such as announcement. Change to the hedex directory and compile with maven:

mvn clean install sakai:deploy

Once built, restart your Tomcat. The code will automatically create the tables
it needs, HDX\_SESSION\_DURATION, HDX\_COURSE\_VISITS, and
HDX\_ASSIGNMENT\_SUBMISSIONS, and then start to populate them as users use the
system.

Configuration
-------------

HEDEX digests Sakai events using a thread pool executor. This starts a pool
of worker threads which are used to handle each event as it comes in. This
starts with an upper limit of 20 threads. You can configure this in your Sakai
properties with:

hedex.digester.threadPoolSize=30

HEDEX returns JSON containing a tenant id. This is initially set to UNSPECIFIED
and if you want it set to something other than that use:

hedex.tenantId=OurSakaiTenantId

When retrieving data, a specific Sakai user must be used as the login. This
defaults to 'hedex-api-user' and can be configured by:

hedex.userId=some-other-user-id

Retrieving Data
---------------

Firstly, have a look at scripts/pulldataexamples.py. To pull the HEDEX data,
your script first needs to login and get a session string. Every call henceforth
passes that session string. The calls will fail otherwise. Data is returned to the 
spec at: http://petstore.swagger.io/?url=http://employees.idatainc.com/~bparish/NoodleBus/swagger_NoodleBus_Retention_API_v4.yaml

To run the script, you'll need python3.x installed, and the SOAP client 'zeep'
installed. You can use pip for that: pip install zeep.
