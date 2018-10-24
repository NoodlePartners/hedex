HEDEX Implementation
--------------------

Hedex installs into Sakai and starts gathering data on user logins, assignment
submissions, and course visits. This data is made available on a set of RESTful
endpoints, authenticated with a particular user based on the requesting agent.

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

HEDEX returns JSON containing a tenant id. This is initially set to UNSPECIFIED
and if you want it set to something other than that use:

hedex.tenantId=OurSakaiTenantId

If you need to, you can disable the digester altogether. This might be useful in
a cluster.

hedex.digester.enabled = false (default: true)

HEDEX uses a site property, 'hedex-agent' to identify a site as being associated
with a particular analytics consumer. For example, Noodle Partners sites will
be marked with the site property 'hedex-agent=noodle'. The digester uses a
scheduled executor to update maps of both the marked sites and their members.
You can configure the interval for this site scan with:

hedex.site.update.interval = 20

The unit is minutes and it defaults to 30

Retrieving Data
---------------

When retrieving data, a specific Sakai user must be used as the login. This
defaults to the reqesting agent, 'noodle' for instance, appended with
'-hedex-user'. The agent should also be passed as the ReqestingAgent parameter.
Without these two things matching, your requests will be rejected.

Firstly, have a look at scripts/pulldataexamples.py. To pull the HEDEX data,
your script first needs to login and get a session string. Every call henceforth
passes that session string. The calls will fail otherwise. Data is returned to the 
spec at: http://petstore.swagger.io/?url=http://employees.idatainc.com/~bparish/NoodleBus/swagger_NoodleBus_Retention_API_v4.yaml

To run the script, you'll need python3.x installed, and the SOAP client 'zeep'
installed. You can use pip for that: pip install zeep.
