
	Instruction How To make requests to ECB API using Postman.
	
	The collection contains requests with examples to create a new node, update the node, create an account, and so on.

1. Verify that Environment working and API-services are running.

	at least:

	ecb-config-registry-winsvc 

	ecb-customer-service-winsvc 

	ecb-security-winsvc 

	ecb-api-zuul-gateway-winsvc 
	
	ecb-foundation-service

	Open  https://localhost:8888/dashboard and verify in "Instances currently registered with Eureka" section visible API-services we will work with. 

2. Install/Setup Postman
	
	Download and install Postman from https://www.getpostman.com/downloads/ 
	
	Run Postman and click File -> close window. Close Tips window 
	
	Click File -> Settings -> SSL certificate verification  -> set OFF. Close Settings tab.
	
	Click 'Import' button -> Import File -> Choose Files -> select "LWAApiExploratoryTestsPostmanCollection.json" 
	
	Click 'Manage Environments' button with gear and click 'Import' button. Select "LWAApiExploratoryTestsPostmanEnvironment.json" 
	
	Click on imported Environment name 'ECB API VM' and change "baseUrl" value to Url where the ECB API services are up and running using port 8711 (ex. https://10.107.3.7:8711 )
		
	Click 'Update' button and close Manage Environment Page.
	
	Select 'ECB API VM' environment 
	
3.  Make requests using Postman
	
	Open collection

	First you need to register client_name and software_id 
	
	Send request 'Token Registration' once. 
	(Note: If any errors in response - try to change "software_id" and "client_name" values in body of request.)
		
	Send request 'Get Token'
	(Node: you will receive token and his life time is 5 minutes. After 5 minutes, you need to get a new token again by request 'Get Token'
	
	Next, you can make requests to create an account, nodes and update existing nodes.
			
	
		
		
		
		

 