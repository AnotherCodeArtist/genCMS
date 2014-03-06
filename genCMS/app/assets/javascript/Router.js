/*
 * clean the main div (#main)
 * -> add class hidden
 * -> remove class hidden from ajax loader div
 * -> clean knockout binding from main div 
 */
var cleanMainNode = function(){
	$('#main').addClass("hidden");
	$('#ajaxLoader').removeClass("hidden");
	ko.cleanNode(document.getElementById("main"));
}

/*
 * apply knockout bindings to main div (#main)
 * -> add class hidden to ajax loader div
 * -> remove class hidden from main div
 * -> activate popover buttons if available
 */
var applyBindingsMainNode = function(viewModel){
	ko.applyBindings(viewModel, document.getElementById("main"));
	$('#ajaxLoader').addClass("hidden");
	$('#main').removeClass("hidden");
	$('[data-toggle=popover]').popover();
}

/*
 * Display the "Forbidden" Page in the main Node
 */
var loadForbiddenPageMainNode = function(){
	$("#main").load("html/forbidden",function() {
		$('#ajaxLoader').addClass("hidden");
		$('#main').removeClass("hidden");
	});
}

/*
 * Client-side routes
 * 
 * Sammy.js is used for routing / navigating back and forward
 */ 
Sammy(function() {
	

	/*
	 * open the user with the provided id for edit
	 */
	this.get('#editUser/:userID', function() {
		if(mainVM.type != 'EditUsersVM'){
			mainVM = new EditUsersVM();
		}
		mainVM.loadUser(this.params.userID,true);
	});

	/*
	 * open the user management page
	 */
	this.get('#users', function() {
		if(mainVM.type != 'EditUsersVM'){
			mainVM = new EditUsersVM();
		}
		mainVM.loadUsersPage(mainVM.currentPage(),true);
	});
	
	/*
	 * create a new Document serverside if called with documentId = 0 and a valid docTypeId
	 */
	this.get('#editDocument/:documentId/:docTypeId', function(){
		mainVM = new EditDocumentVM();
		cleanMainNode();
		$.getJSON("/document/"+this.params.documentId+"/"+this.params.docTypeId,function(allData) {
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
		});
	});
	
	/*
	 * Open the Document Edit page for the specified Document
	 */
	this.get('#editDocument/:documentId', function(){
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
				}
			});
		}else{
			//load data and editor
			mainVM = new EditDocumentVM();
			$.getJSON("/document/"+this.params.documentId, function(allData) {
				if (allData.res == "OK") {
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
						}
					});
				} else {
					showMessage(allData.error.error, "danger");
				}
			});
		}
	});
	
	/*
	 * load the users documents list from the server and displays them
	 */
	this.get('#myDocuments', function() {
		if(mainVM.type != 'MyDocumentsVM'){
			mainVM = new MyDocumentsVM();
		}
		mainVM.loadMyDocumentsPage(headVM.currentPage(),true);
	});
	
	/*
	 * load the unreleased documents list from the server and displays them
	 */
	this.get('#unreleasedDocuments', function() {
		if(mainVM.type != 'UnreleasedDocumentsVM'){
			mainVM = new UnreleasedDocumentsVM();
			headVM.currentPage(0);
		}
		mainVM.loadUnreleasedDocumentsPage(headVM.currentPage(),true);
	});
	
	/*
	 * load the doctypes from the server and the view if needed
	 */
	this.get('#docTypes', function() {
		if(mainVM.type != 'DocTypeVM'){
			mainVM = new DocTypeVM();
		}
		//load data from server (and view html if needed)
		mainVM.loadDocTypePage(mainVM.currentPage());
	});

	/*
	 * load the data of a single doctype and the view if needed
	 */
	this.get('#editDocType/:docTypeId',function() {
		if(mainVM.type != 'DocTypeVM'){
			mainVM = new DocTypeVM();
		}
		$.getJSON("/doctype/"+ this.params.docTypeId,function(allData) {
			mainVM.selectedDocType(ko.mapping.fromJS(allData));
			if($("#docTypeEdit").length == 0){ //load docType Editor
				cleanMainNode();
				$("#main").load("html/docTypeEditor",function(response, status, xhr) {
					if(status == "error"){
						//console.log("User is not authorized for editing doctypes!")
						cleanMainNode();
						loadForbiddenPageMainNode();
					}else{
						applyBindingsMainNode(mainVM);
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
	
	/*
	 * load the main design editor for the document type and the design editor
	 */
	this.get('#editDocTypeDesign/:docTypeId', function() {
		if(mainVM.type != 'DocTypeVM'){
			mainVM = new DocTypeVM();
		}
		var id = this.params.docTypeId;
		cleanMainNode();
		$.getJSON("/doctype/"+id,function(allData) {
			mainVM.selectedDocType(ko.mapping.fromJS(allData));
			$.getJSON("/doctype/styles",function(allData) {
				mainVM.availableStyles(allData.data.styles);
				//load Editor
				$("#main").load("html/docTypeDesignEditor",function(response, status, xhr) {
					if(status == "error"){
						cleanMainNode();
						loadForbiddenPageMainNode();
					}else{
						applyBindingsMainNode(mainVM);
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
	
	/*
	 * load the document type list design and the design editor
	 */
	this.get('#editDocTypeListDesign/:docTypeId', function() {
		if(mainVM.type != 'DocTypeVM'){
			mainVM = new DocTypeVM();
		}else{
			//console.log("use existing DocTypeVM");
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
						//console.log("User is not authorized for editing doctypes!")
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
	
	/*
	 * open the view for connecting the docType to the selectedProject
	 */
	this.get('#docTypeConnect/:docTypeId', function() {
		if(mainVM.type != 'DocTypeVM'){
			//console.log("create new DocTypeViewmodel");
			mainVM = new DocTypeVM();
		}else{
			//console.log("use existing DocTypeVM");
		}
		//load docType (for elements and locales)
		var id = this.params.docTypeId;
		//self.showAjaxLoader('show');
		cleanMainNode();
		$.getJSON("/doctype/"+id,function(allData) {
			//load Editor
			mainVM.docTypeConnection(ko.mapping.fromJSON('{"name":"","description":"","active":false}'));
			mainVM.selectedDocType(ko.mapping.fromJS(allData));
			$("#main").load("/html/docTypeConnecter",function(response, status, xhr) {
				if(status == "error"){
					//console.log("User is not authorized for editing doctypes!")
					cleanMainNode();
					loadForbiddenPageMainNode();
				}else{
					applyBindingsMainNode(mainVM);
				}
			});
		});
	});
	
	/*
	 * display a list of the available Document Types (connections between doc Types and the current project)
	 */
	this.get('#createDocument',function() {
		cleanMainNode();
		//ko.cleanNode(document.getElementById("main"));
		$.getJSON("/doctype/connected", function(allData) {
			if(allData.res == "OK"){
				$("#main").load("html/selectNewDocument",function(response, status, xhr) {
					if(status == "error"){
						//console.log("User is not authorized for editing doctypes!")
						cleanMainNode();
						loadForbiddenPageMainNode();
					}else{
						mainVM = new CreateDocumentVM(allData.data);
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
	
	/*
	 * display a list of the available Documenttypes (connections between doc Types and the current project)
	 */
	this.get('#createDocument/:docToConnect',function() {
		cleanMainNode();
		var docToConnect = this.params.docToConnect;
		$.getJSON("/doctype/connected", function(allData) {
			console.log("connected documenttypes:");
			if(allData.res == "OK"){
				$("#main").load("html/selectNewDocument",function(response, status, xhr) {
					if(status == "error"){
						//console.log("User is not authorized for editing doctypes!")
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
	
	/*
	 * Display a page for the creation of a new project
	 * Input required is the title of the new project
	 */
	this.get('#newProject',function() {
		cleanMainNode();
		$("#main").load("html/newProject", function() {
			mainVM = new ProjectNewVM();
			applyBindingsMainNode(mainVM);
		});
	});
	
	/*
	 * Display the project edit page
	 */
	this.get('#editProject',function() {
		cleanMainNode();
		$.getJSON("/project", function(allData) {
			var selectedProject = ko.mapping.fromJS(allData.data.project);
			var orderedTags = allData.data.ordererdTags;
			var styles = allData.data.styles;
			console.log("loaded project: ");
			console.log(selectedProject);
			//load projectEditor HTML from Server
			$("#main").load("html/projectEdit", function() {
				mainVM = new ProjectEditVM(selectedProject, orderedTags, styles);
				applyBindingsMainNode(mainVM);
				mainVM.init();
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
	
	/*
	 * Display a List of all available projects (title + description)
	 * User can select one
	 */
	this.get('/#selectProject',function() {
		cleanMainNode();
		$.getJSON("/projects", function(allData) {
			var observableData = ko.mapping.fromJS(allData);
			//load projectList HTML from Server
				$("#main").load("html/projects", function() {
					mainVM = new ProjectSelectVM(observableData());
					applyBindingsMainNode(mainVM);
				});
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
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
	
	/*
	 * Display the index page of the project / go to select project page if no project is selected
	 */
	this.get('/#index', function(){
		if(headVM.initialized){
			headVM.selectedTag("");
			if(headVM.selectedProject()===""){
				//console.log("goto selectpr");
				//no project selected, go to select page
				headVM.goToSelectProject();
			}else{
				if(mainVM.type != 'UserViewVM'){
					//console.log("create new UserViewVM");
					mainVM = new UserViewVM();
					mainVM.loadIndexPage(0, true, true);
				}else{
					//console.log("use existing UserViewVM");
					mainVM.loadIndexPage(0, true, false);
				}
			}
		}else{
			location.hash="";
		}
	});
	
	/*
	 * Display the document search map 
	 */
	this.get('/#map', function(){
		if(headVM.initialized){
			headVM.selectedTag("");
			if(headVM.selectedProject()===""){
				//console.log("goto selectpr");
				//no project selected, go to select page
				headVM.goToSelectProject();
			}else{
				if(mainVM.type != 'UserViewVM'){
					//console.log("create new UserViewVM");
					mainVM = new UserViewVM();
					//console.log("open");
					mainVM.loadNewestDocuments(true,true);
				}else{
					//console.log("use existing UserViewVM");
					mainVM.openMapSearch();
				}
			}
		}else{
			location.hash="";
		}
	});

	/*
	 * display a document detail view
	 */
	this.get('#doc/:docID', function(){
		if(headVM.initialized){
			console.log("detail doc");
			if(headVM.selectedProject()===""){
				//no project selected, go to select page
				headVM.goToSelectProject();
			}else{
				if(mainVM.type != 'UserViewVM'){
					//console.log("create new UserViewVM");
					mainVM = new UserViewVM();
					//load page (html) and doc
					mainVM.loadDocument(this.params.docID,true);
				}else{
					//console.log("use existing UserViewVM");
					//load page only
					mainVM.loadDocument(this.params.docID,false);
				}
			}
		}else{
			location.hash="";
		}
		
	});
	
	/*
	 * display list with specified tag only
	 */
	this.get('#docs/:tagName', function(){
		//console.log("docs with tagName "+ decodeURIComponent(this.params.tagName));
		headVM.selectedTag(this.params.tagName);
		if(mainVM.type != 'UserViewVM'){
			//console.log("create new UserViewVM");
			mainVM = new UserViewVM();
			mainVM.loadIndexPage(0, true, true);
		}else{
			//console.log("use existing UserViewVM");
			mainVM.loadIndexPage(0, true, false);
		}
	});
	
}).run();