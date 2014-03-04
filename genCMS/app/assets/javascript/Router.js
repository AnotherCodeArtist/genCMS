var cleanMainNode = function(){
	$('#main').addClass("hidden");
	$('#ajaxLoader').removeClass("hidden");
	ko.cleanNode(document.getElementById("main"));
}

var applyBindingsMainNode = function(viewModel){
	ko.applyBindings(viewModel, document.getElementById("main"));
	$('#ajaxLoader').addClass("hidden");
	$('#main').removeClass("hidden");
	//activate popover buttons if there are any
	$('[data-toggle=popover]').popover();
}

var loadForbiddenPageMainNode = function(){
	$("#main").load("html/forbidden",function() {
		$('#ajaxLoader').addClass("hidden");
		$('#main').removeClass("hidden");
	});
}



// Client-side routes
Sammy(function() {
	
	/*
	this.get('#login', function() {
		ko.cleanNode(document.getElementById("main"));
		$("#main").load("html/login");
		//ko.applyBindings(viewModel, document.getElementById("main"));
	});
	*/

	//TODO
	this.get('#editUser/:userID', function() {
		if(mainVM.type != 'EditUsersVM'){
			mainVM = new EditUsersVM();
		}
		mainVM.loadUser(this.params.userID,true);
/*		var url = "user/"+this.params.userID;
		//load user json
		$.getJSON(url, function(allData) {
			console.log("response from "+url);
			console.log(allData);
			if(allData.res=="OK"){
				mainVM.selectedUser(ko.mapping.fromJS(allData.data.user));
				mainVM.projectID(allData.data.projectID);
				//load document list html from Server
				cleanMainNode();
				$("#main").load("html/userSettings",function() {
					applyBindingsMainNode(mainVM);
				});
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing doctypes!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
		*/
	});


	this.get('#users', function() {
		if(mainVM.type != 'EditUsersVM'){
			mainVM = new EditUsersVM();
		}
		mainVM.loadUsersPage(mainVM.currentPage(),true);
	});
	
	//TODO
	this.get('#editDocument/:documentId/:docTypeId', function(){
		mainVM = new EditDocumentVM();
		cleanMainNode();
		console.log(this.params.documentId);
		console.log(this.params.docTypeId);
		//Create new Document on Server
		
		$.getJSON("/document/"+this.params.documentId+"/"+this.params.docTypeId,function(allData) {
			//mainVM.docType(ko.mapping.fromJS(allData));
			if (allData.res == "OK") {
				console.log(allData);
				mainVM.document(ko.mapping.fromJS(allData.data.document));
				mainVM.elements(ko.mapping.fromJS(allData.data.elements));
				mainVM.template(allData.data.template);
				mainVM.listTemplate(allData.data.listTemplate);
				$("#main").load("html/documentEditor",function() {
					applyBindingsMainNode(mainVM);
					//initPolyfill for Number Input
					$(':input[type="number"]').inputNumber();
				});
			} else {
				showMessage(allData.error, "danger");
			}
			//var elements = allData.elems;
//			var document = new Object();
//			elements.forEach(function(element) {
//				console.log(element.fname);
//				document[element.fname] = "";
//			});
//			console.log(document);
		});
		//TODO
		/*
		 * load new Document from server (based on docTypeId)
		 * load elements from DocType from Server
		 * Display Editor
		 */
		//location.hash = "editDocument/0/"+data.docTypeId;
	});
	
	//TODO
	this.get('#editDocument/:documentId', function(){
		console.log("EDIT DOCUMENT: "+this.params.documentId);
		cleanMainNode();
		if(mainVM.type === 'EditDocumentVM' && mainVM.currentDocumentID() == this.params.documentId){
			//document already loaded - load editor and display
			console.log("document already loaded - load editor");
			$("#main").load("html/documentEditor",function(response, status, xhr) {
				if(status == "error"){
					console.log("User is not authorized for editing doctypes!")
					cleanMainNode();
					loadForbiddenPageMainNode();
				}else{
					applyBindingsMainNode(mainVM);
					//initPolyfill for Number Input
					//$(':input[type="number"]').inputNumber();
				}
			});
		}else{
			//load data and editor
			console.log("document not loaded - load data and editor");
			mainVM = new EditDocumentVM();
			$.getJSON("/document/"+this.params.documentId, function(allData) {
				//mainVM.docType(ko.mapping.fromJS(allData));
				if (allData.res == "OK") {
					console.log(allData);
					mainVM.currentDocumentID(allData.data.document._id.$oid);
					mainVM.document(ko.mapping.fromJS(allData.data.document));
					mainVM.elements(ko.mapping.fromJS(allData.data.elements));
					mainVM.template(allData.data.template);
					mainVM.listTemplate(allData.data.listTemplate);
					$("#main").load("html/documentEditor",function(response, status, xhr) {
						if(status == "error"){
							console.log("User is not authorized for editing doctypes!")
							cleanMainNode();
							loadForbiddenPageMainNode();
						}else{
							applyBindingsMainNode(mainVM);
							//initPolyfill for Number Input
							//$(':input[type="number"]').inputNumber();
							//initMap();
						}
					});
				} else {
					showMessage(allData.error.error, "danger");
				}
			});
		}
	});
	
	/**
	 * loads the users documents list from the server and displays them
	 */
	this.get('#myDocuments', function() {
		if(mainVM.type != 'MyDocumentsVM'){
			mainVM = new MyDocumentsVM();
		}
		mainVM.loadMyDocumentsPage(headVM.currentPage(),true);
	});
	
	/**
	 * loads the unreleased documents list from the server and displays them
	 */
	this.get('#unreleasedDocuments', function() {
		if(mainVM.type != 'UnreleasedDocumentsVM'){
			mainVM = new UnreleasedDocumentsVM();
			headVM.currentPage(0);
		}
		mainVM.loadUnreleasedDocumentsPage(headVM.currentPage(),true);
	});
	
	/**
	 * loads the doctypes from the server and the view if needed
	 */
	this.get('#docTypes', function() {	//#
		if(mainVM.type != 'DocTypeVM'){
			mainVM = new DocTypeVM();
		}
		//load data from server (and view html if needed)
		mainVM.loadDocTypePage(mainVM.currentPage());
	});

	/**
	 * loads the data of a single doctype and the view if needed
	 */
	this.get('#editDocType/:docTypeId',function() {	//#
		if(mainVM.type != 'DocTypeVM'){
			mainVM = new DocTypeVM();
		}
		$.getJSON("/doctype/"+ this.params.docTypeId,function(allData) {
			mainVM.selectedDocType(ko.mapping.fromJS(allData));
			//TODO check if editor is already loaded - if not load it
			if($("#docTypeEdit").length == 0){ //load docType Editor
				cleanMainNode();
				//ko.cleanNode(document.getElementById("main"));
				$("#main").load("html/docTypeEditor",function(response, status, xhr) {
					if(status == "error"){
						console.log("User is not authorized for editing doctypes!")
						cleanMainNode();
						loadForbiddenPageMainNode();
					}else{
						applyBindingsMainNode(mainVM);
						//ko.applyBindings(mainVM, document.getElementById("main"));
						$("#docTypeList").hide();
						$("#docTypeEdit").show();
					}
				});
			}else{
				$("#docTypeList").hide();
				$("#docTypeEdit").show();
			}
		});
	});
	
	/**
	 * load the main design editor for the document type and the design editor
	 */
	this.get('#editDocTypeDesign/:docTypeId', function() {
		if(mainVM.type != 'DocTypeVM'){
			mainVM = new DocTypeVM();
		}
		//load docType (for elements and locales)
		var id = this.params.docTypeId;
		//self.showAjaxLoader('show');
		cleanMainNode();
		//ko.cleanNode(document.getElementById("main"));
		$.getJSON("/doctype/"+id,function(allData) {
			mainVM.selectedDocType(ko.mapping.fromJS(allData));
			$.getJSON("/doctype/styles",function(allData) {
				mainVM.availableStyles(allData.data.styles);
			//load Editor
			$("#main").load("html/docTypeDesignEditor",function(response, status, xhr) {
				if(status == "error"){
					console.log("User is not authorized for editing doctypes!")
					cleanMainNode();
					loadForbiddenPageMainNode();
				}else{
					applyBindingsMainNode(mainVM);
					//ko.applyBindings(mainVM, document.getElementById("main"));
					//TODO Load design JSON
					mainVM.initDesignEditor();
					$.getJSON("/doctypeDesign/"+id,function(allData) {
						if(allData.design!=null){
							mainVM.rebuildDesignHTML(allData.design);
							mainVM.initDesignEditor();
						}
					});
				}
			});
		});
		});
	});
	
	/**
	 * load the list design and the design editor
	 */
	this.get('#editDocTypeListDesign/:docTypeId', function() {
		if(mainVM.type != 'DocTypeVM'){
			console.log("create new DocTypeViewmodel");
			mainVM = new DocTypeVM();
		}else{
			console.log("use existing DocTypeVM");
		}
		//load docType (for elements and locales)
		var id = this.params.docTypeId;
		//self.showAjaxLoader('show');
		$.getJSON("/doctype/"+id,function(allData) {
			mainVM.selectedDocType(ko.mapping.fromJS(allData));
			$.getJSON("/doctype/styles",function(allData) {
				mainVM.availableStyles(allData.data.styles);
			//load Editor
			cleanMainNode();
			//ko.cleanNode(document.getElementById("main"));
			$("#main").load("html/docTypeListDesignEditor",function(response, status, xhr) {
				if(status == "error"){
					console.log("User is not authorized for editing doctypes!")
					cleanMainNode();
					loadForbiddenPageMainNode();
				}else{
					applyBindingsMainNode(mainVM);
					//ko.applyBindings(mainVM, document.getElementById("main"));
					mainVM.initDesignEditor();
					$.getJSON("/doctypeListDesign/"+id,function(allData) {
						if(allData.listDesign!=null){
							mainVM.rebuildDesignHTML(allData.listDesign);
							mainVM.initDesignEditor();
						}
					});
				}
			});
			});
		});
	});
	
	/**
	 * opens the view for connecting the docType to the selectedProject
	 */
	this.get('#docTypeConnect/:docTypeId', function() {
		if(mainVM.type != 'DocTypeVM'){
			console.log("create new DocTypeViewmodel");
			mainVM = new DocTypeVM();
		}else{
			console.log("use existing DocTypeVM");
		}
		//load docType (for elements and locales)
		var id = this.params.docTypeId;
		//self.showAjaxLoader('show');
		cleanMainNode();
		//ko.cleanNode(document.getElementById("main"));
		$.getJSON("/doctype/"+id,function(allData) {
			//load Editor
			mainVM.docTypeConnection(ko.mapping.fromJSON('{"name":"","description":"","active":false}'));
			mainVM.selectedDocType(ko.mapping.fromJS(allData));
			$("#main").load("/html/docTypeConnecter",function(response, status, xhr) {
				if(status == "error"){
					console.log("User is not authorized for editing doctypes!")
					cleanMainNode();
					loadForbiddenPageMainNode();
				}else{
					applyBindingsMainNode(mainVM);
					//ko.applyBindings(mainVM, document.getElementById("main"));
				}
			});
		});
	});
	
	/**
	 * displays a list of the available Documenttypes (connections between doc Types and the current project)
	 */
	this.get('#createDocument',function() {
		console.log("Auswahl für Erstellung eines neuen Dokuments");
		cleanMainNode();
		//ko.cleanNode(document.getElementById("main"));
		$.getJSON("/doctype/connected", function(allData) {
			console.log("connected documenttypes:");
			if(allData.res == "OK"){
				console.log(allData.data);
				//self.availableDocTypes = ko.observableArray(allData.data);
				$("#main").load("html/selectNewDocument",function(response, status, xhr) {
					if(status == "error"){
						console.log("User is not authorized for editing doctypes!")
						cleanMainNode();
						loadForbiddenPageMainNode();
					}else{
						mainVM = new CreateDocumentVM(allData.data);
						//ko.applyBindings(mainVM, document.getElementById("main"));
						applyBindingsMainNode(mainVM);
					}
				});
			}else {
				console.log("connected Doctypes could not be loaded");
				console.log(allData);
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				console.log("User is not authorized for editing doctypes!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	});
	
	/**
	 * displays a list of the available Documenttypes (connections between doc Types and the current project)
	 */
	this.get('#createDocument/:docToConnect',function() {
		console.log("Auswahl für Erstellung eines neuen verknüpften Dokuments");
		cleanMainNode();
		var docToConnect = this.params.docToConnect;
		//ko.cleanNode(document.getElementById("main"));
		$.getJSON("/doctype/connected", function(allData) {
			console.log("connected documenttypes:");
			if(allData.res == "OK"){
				console.log(allData.data);
				//self.availableDocTypes = ko.observableArray(allData.data);
				$("#main").load("html/selectNewDocument",function(response, status, xhr) {
					if(status == "error"){
						console.log("User is not authorized for editing doctypes!")
						cleanMainNode();
						loadForbiddenPageMainNode();
					}else{
						mainVM = new CreateDocumentVM(allData.data);
						mainVM.connectToDocument(docToConnect);
						applyBindingsMainNode(mainVM);
					}
				});
			}else {
				console.log("connected Doctypes could not be loaded");
				console.log(allData);
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				console.log("User is not authorized for editing doctypes!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	});
	
	/**
	 * Displays a page for the creation of a new project
	 * Input required is the title of the new project
	 */
	this.get('#newProject',function() {
		console.log("Erstelle ein neues Projekt");
		cleanMainNode();
		//ko.cleanNode(document.getElementById("main"));
		$("#main").load("html/newProject", function() {
			mainVM = new ProjectNewVM();
			applyBindingsMainNode(mainVM);
			//ko.applyBindings(mainVM, document.getElementById("main"));
		});
	});
	
	/**
	 * Displays the project edit page
	 */
	this.get('#editProject',function() {
		console.log("Bearbeite das aktuell ausgewählte Projekt");
		//ko.cleanNode(document.getElementById("main"));
		cleanMainNode();
		$.getJSON("/project", function(allData) {
			console.log(allData);
			var selectedProject = ko.mapping.fromJS(allData.data.project);
			//var distinctTags = allData.data.project.tags;//allData.data.distinctTags;
			var orderedTags = allData.data.ordererdTags;
			var styles = allData.data.styles;
			console.log("loaded project: ");
			console.log(selectedProject);
			//load projectEditor HTML from Server
			$("#main").load("html/projectEdit", function() {
				mainVM = new ProjectEditVM(selectedProject, orderedTags, styles);
				applyBindingsMainNode(mainVM);
				mainVM.init();
				//ko.applyBindings(mainVM, document.getElementById("main"));
			});
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing doctypes!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	});
	
	/**
	 * Display a List of all available projects (title + description)
	 * User can select one
	 */
	this.get('/#selectProject',function() {
		cleanMainNode();
		//ko.cleanNode(document.getElementById("main"));
		$.getJSON("/projects", function(allData) {
			var observableData = ko.mapping.fromJS(allData);
			console.log("loaded projects: ");
			console.log(observableData());
			//load projectList HTML from Server
				$("#main").load("html/projects", function() {
					mainVM = new ProjectSelectVM(observableData());
					applyBindingsMainNode(mainVM);
					//ko.applyBindings(mainVM, document.getElementById("main"));
				});
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing doctypes!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	});

	this.get('/', function(){
		if(headVM.initialized){
			headVM.goToIndex();
		}
	});
	
	this.get('/#index', function(){
		if(headVM.initialized){
			headVM.selectedTag("");
			if(headVM.selectedProject()===""){
				console.log("goto selectpr");
				//no project selected, go to select page
				headVM.goToSelectProject();
			}else{
				if(mainVM.type != 'UserViewVM'){
					console.log("create new UserViewVM");
					mainVM = new UserViewVM();
					mainVM.loadIndexPage(0, true, true);
				}else{
					console.log("use existing UserViewVM");
					mainVM.loadIndexPage(0, true, false);
				}
			}
		}else{
			location.hash="";
		}
	});
	
	this.get('/#map', function(){
		if(headVM.initialized){
			headVM.selectedTag("");
			if(headVM.selectedProject()===""){
				console.log("goto selectpr");
				//no project selected, go to select page
				headVM.goToSelectProject();
			}else{
				if(mainVM.type != 'UserViewVM'){
					console.log("create new UserViewVM");
					mainVM = new UserViewVM();
					console.log("open");
					mainVM.loadNewestDocuments(true,true);
					//mainVM.openMapSearch();
					//mainVM.loadIndexPage(0, true, true);
				}else{
					console.log("use existing UserViewVM");
					mainVM.openMapSearch();
					//mainVM.loadIndexPage(0, true, false);
				}
			}
		}else{
			location.hash="";
		}
	});

	this.get('#doc/:docID', function(){
		if(headVM.initialized){
			console.log("detail doc");
			if(headVM.selectedProject()===""){
				console.log("goto selectpr");
				//no project selected, go to select page
				headVM.goToSelectProject();
			}else{
				if(mainVM.type != 'UserViewVM'){
					console.log("create new UserViewVM");
					mainVM = new UserViewVM();
					mainVM.loadDocument(this.params.docID,true);
					//load page (html) and doc
				}else{
					console.log("use existing UserViewVM");
					//load page only
					mainVM.loadDocument(this.params.docID,false);
				}
			}
		}else{
			location.hash="";
		}
		
	});
	
	this.get('#docs/:tagName', function(){
		console.log("docs with tagName "+ decodeURIComponent(this.params.tagName));
		headVM.selectedTag(this.params.tagName);
		if(mainVM.type != 'UserViewVM'){
			console.log("create new UserViewVM");
			mainVM = new UserViewVM();
			mainVM.loadIndexPage(0, true, true);
		}else{
			console.log("use existing UserViewVM");
			mainVM.loadIndexPage(0, true, false);
		}
	});
	/*this.get('/login',function(){
		console.log('login called');
		location.replace(this.path);
	});*/
	
	this.get('/register',function(){
		console.log('register called');
		//location.hash = "login";
	});
	
	/*this.notFound = function() {
		// do something
		console.log("not found");
		console.log(location.hash);
		$("#main").load("html/404", function() {
		});
		console.log('404!');
	  }*/
	// this.get('#404', function(){

	// });

	// this.notFound = function(){
	//
	// }

	/*
	 * this.get('', function() { this.app.runRoute("get","#index");
	 * });
	 * 
	 * this.get('#index', function() { location.assign("index"); }); //
	 * this.get('#', function() { // // }); /*this.get("#Index",
	 * function(){ $("#main").load("/"); }); /*
	 */
}).run();