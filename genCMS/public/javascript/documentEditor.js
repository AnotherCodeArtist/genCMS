function DocumentType(data) {
	this.author = ko.observable(data.author);
	this.name = ko.observable(data.name);
	this.linkedDocuments = ko.observableArray(data.linkedDocuments);
	this.singleLineElems = ko.observableArray(data.singleLine);
	this.htmlTextElems = ko.observableArray(data.htmlTextElems);
	this.createdAt = ko.observable(data.createdAt);
	this.modifiedAt = ko.observable(data.modifiedAt);
	this.id = ko.observable(data._id);
	console.log(data._id)
	/*
	 * author: String, name: String, linkedDocuments: List[BSONObjectID] = Nil,
	 * singleLineElems: List[SingleLineText] = Nil, htmlTextElems:
	 * List[HtmlText] = Nil, createdAt: Long=0, modifiedAt: Long=0, _id:
	 * BSONObjectID = BSONObjectID.generate)
	 */
	
}

function DocumentTypeViewModel() {
	//Data
	var self = this;
	self.docTypes = ko.observableArray([]);
	
	
	//Operations
	
	//Load initial State from server
	$.getJSON("/doctype/all", function(allData){
		var mappedDocTypes = $.map(allData, function(item){ return new DocumentType(item)});
		self.docTypes(mappedDocTypes);
	});
}

ko.applyBindings(new DocumentTypeViewModel());