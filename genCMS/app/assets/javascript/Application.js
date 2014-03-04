// ------------------------------------------------------------
// Custom Bindings

var genCMSseperator = "###genCMS###";
var spacer = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

ko.bindingHandlers.genCMSnavigation = {
	    update: function(element, valueAccessor) {
	    	console.log("#######UPDATE HEAD NAVIGATION!!!!!!!!!!!!!");
//	    	console.log(element);
//	    	console.log(valueAccessor());
//	    	console.log(ko.mapping.toJS(valueAccessor));
//	    	console.log(ko.mapping.toJS(valueAccessor()));
	    	//build navigation html
	    	//set navigation html
			var html = "";
	    	$.each(ko.mapping.toJS(valueAccessor()),function(){
			 var tagName = this.tag;
			 var childs = this.childs;
			 var parentStartHTML = "";
			 var parentEndHTML = "";
			 var childrenHTML = "";
//			 console.log("childs");
//			 console.log(childs);
			 if(childs == null || childs.length == 0){
				 var count = headVM.getTagCount(tagName);
				 if(count > 0){
					 html += '<li><a class="tagNavigationLink" href="#docs/'+encodeURIComponent(tagName)+'" tagname="'+tagName+'">';
					 html += '<span class="badge navBadge">'+count+'</span>'+tagName+spacer;
					 html += '</a></li>';	
				 }
			 }else{
				 parentStartHTML += '<li class="dropdown"><a href="#" class="dropdown-toggle" data-toggle="dropdown">'+tagName+' <i class="fa fa-caret-down fa-lg"></i></a>';
				 parentStartHTML += '<ul class="dropdown-menu span10">';
				 $.each(childs,function(){
					 var childName = this.toString();
//					 console.log("thischild: "+childName);
//					 console.log(childName);
//					 console.log(typeof childName);
					 var childCount = headVM.getTagCount(childName);
//					 console.log(childCount);
					 if(childCount > 0){
						 childrenHTML += '<li><a class="tagNavigationLink tagNavigationChildLink" href="#docs/'+encodeURIComponent(childName)+'" tagname="'+childName+'">';
						 childrenHTML += '<span class="badge navBadge">'+childCount+'</span>'+childName+spacer;
						 childrenHTML += '</a></li>';
					 }
				 });
				 parentEndHTML += '<li class="divider"></li></ul></li>';
//				 console.log(parentStartHTML + childrenHTML + parentEndHTML);
				if(childrenHTML != ""){	//display of dropdown makes only sence if there are children to display/click				
					html += parentStartHTML + childrenHTML + parentEndHTML;
				} 
			 }
			});
    		$(element).html(html);
	    }
	};

ko.bindingHandlers.genCMStemplateList = {
    init: function(element, valueAccessor) {
//    	console.log("#######INIIIIT");
//    	console.log(element);
//    	console.log(valueAccessor());
    	
    },
    update: function(element, valueAccessor) {
//    	console.log("#######UPDATE");
//    	console.log(element);
//    	console.log(valueAccessor());
//    	console.log(ko.mapping.toJS(valueAccessor));
//    	console.log(ko.mapping.toJS(valueAccessor()));
    	var document = ko.mapping.toJS(valueAccessor());
    	var connection = document.connection;
    	var fields = document.fields;
//    	console.log("fields");
    	if(typeof fields != "undefined"){
    		if(typeof fields.geoloc != "undefined"){
    			var lon = fields.geoloc.lon;
    			var lat = fields.geoloc.lat;
    			var display_name = fields.geoloc.display_name;
    			fields.geoloc = lon + genCMSseperator + lat + genCMSseperator + display_name + genCMSseperator + 0.1;
    		}
    		//headVM.labelLocales()[headVM.projectConnectionTemplates["53104c136500008c05f1357a"].docType][headVM.currentLocale]
    		fields.genCMSlabel = headVM.labelLocales()[headVM.projectConnectionTemplates[document.connection].docType][headVM.currentLocale];
    		//headVM.projectConnectionTemplates[document.connection].list(fields))
    		//add locale for labels
    		//headVM.currentLocal
    		//self.labelLocales(allData.data.localeLabels);
    		
    		
    		$(element).html(headVM.projectConnectionTemplates[document.connection].list(fields));
    		initAudioPlayersOnPage();
    		initVideoPlayersOnPage();
    		initImagesOnPage();
    		setTimeout(function(){
    			initMapsOnPage();
    		},300);
    	}else{
    		console.log("FIELDS IS NOT DEFINED!");
    	}
    }
};

ko.bindingHandlers.genCMStemplateFull = {
	    update: function(element, valueAccessor) {
//	    	console.log("#######UPDATE");
//	    	console.log(element);
//	    	console.log(valueAccessor());
//	    	console.log(ko.mapping.toJS(valueAccessor));
//	    	console.log(ko.mapping.toJS(valueAccessor()));
	    	var document = ko.mapping.toJS(valueAccessor());
	    	var fields = document.fields;
//	    	console.log("fields");
	    	if(typeof fields != "undefined"){
	    		if(typeof fields.geoloc != "undefined"){
	    			var lon = fields.geoloc.lon;
	    			var lat = fields.geoloc.lat;
	    			var display_name = fields.geoloc.display_name;
	    			fields.geoloc = lon + genCMSseperator + lat + genCMSseperator + display_name + genCMSseperator + 0.2;
	    		}
	    		if(typeof fields.modifiedAt != "undefined"){
	    			fields.modifiedAt = timeStampToDate(fields.modifiedAt.$date,headVM.currentLocale);
	    		}
	    		if(typeof fields.createdAt != "undefined"){
	    			fields.createdAt = timeStampToDate(fields.createdAt.$date,headVM.currentLocale);
	    		}
//	    		console.log(fields);
	    		fields.genCMSlabel = headVM.labelLocales()[headVM.projectConnectionTemplates[document.connection].docType][headVM.currentLocale];
	    		//headVM.projectConnectionTemplates[document.connection].list(fields))
	    		$(element).html(headVM.projectConnectionTemplates[document.connection].full(fields));
	    		initAudioPlayersOnPage();
	    		initVideoPlayersOnPage();
	    		initImagesOnPage();
	    		setTimeout(function(){
	    			initMapsOnPage();
	    		},300);
	    	}else{
	    		console.log("FIELDS IS NOT DEFINED!");
	    	}
	    }
	};

//ko.bindingHandlers.genCMSviewMap = {
//	    init: function(element, valueAccessor) {
////	    	console.log("#######INIIIIT");
////	    	console.log(element);
////	    	console.log(valueAccessor());
////	    	var id =  $(element).attr('id');
////	    	$(element).parent().removeClass("hidden");
////	    	//TODO Default from Project lat, lon
////	    	setTimeout(function(){
////		    	var mapExists = initMap(id, 47.0786521, 15.4070155, 13);
////		    	if(mapExists){
////					// add click handler for address lookup
////					/*map.on('click', function(e) {
////						console.log("Lat, Lon : " + e.latlng.lat + ", " + e.latlng.lng);
////						$('#addressInput').val(e.latlng.lat + ", " + e.latlng.lng);
////					})*/;
////					// add resize event to map
////					$(window).on("resize", function() {
////						$("#map").height($(window).height()*0.5);
////						map.invalidateSize();
////					}).trigger("resize");			
////					// check if location is given - if so - add marker to map!
////					// clean lookedUpAddresses
////					askForLocationsOnMap();
////					map.on('moveend', onMapMove);
////		    	}
////	    	},400);
//	    	//$(element).parent().addClass("hidden");
//	
//	    },
//	    update: function(element, valueAccessor) {
////	    	console.log("#######UPDATE");
//	    	//$(element).html(valueAccessor());
//	    	//initAudioPlayersOnPage();
//			//initVideoPlayersOnPage();
//	    }
//	};

ko.bindingHandlers.genCMStagCloud = {
    init: function(element, valueAccessor) {
//    	console.log("#######INIIIIT");
//    	console.log(element);
//    	console.log(valueAccessor);
//    	console.log(valueAccessor());
//    	console.log(valueAccessor()());
    	
    },
    update: function(element, valueAccessor) {
//    	console.log("#######UPDATE");
    	
    	var tagTotalCount = valueAccessor()().length;
//    	console.log("total_: "+tagTotalCount);
    	var maxCount = valueAccessor()()[0].count;
//    	console.log("maxCount: "+maxCount);
//    	console.log(valueAccessor()()[0]);
//    	console.log(valueAccessor()()[1]);
//    	console.log(valueAccessor()()[2]);
//    	console.log(valueAccessor()()[3]);
    	var minCount = valueAccessor()()[(tagTotalCount-1)].count;
    	var dist = (maxCount - minCount)/3;
    	var one = dist;
    	var two = one+dist;
    	var three = two+dist;
    	var html = '<div id="tagCloud"><p>';
    	$.each(valueAccessor()(),function(){
//    		console.log(this._id);
//    		console.log(this.count);
    		var tagClass = "tagLarge";
    		if(this.count <= one){
    			tagClass = "tagSmall";
    		}else if(this.count <= two){
    			tagClass = "tagNormal";
    		}else if(this.count <= three){
    			tagClass = "tagMedium";
    		}
    		html = html + '<a class="btn '+tagClass+' addTagToFilter" tagName="'+this._id+'">'+this._id+'</a>  ';
    	});
    	
    	html = html + '</p></div>';
    	if(headVM.filterTags().length > 0){
	    	html = html + '<hr><div class="tags">';
	    	$.each(headVM.filterTags(), function(){
	    		html = html + '<button class="btn btn-default btn-sm btn-tag-remove removeTagFromFilter" tagName="'+this+'"><i class="fa fa-ban fa-sm"></i> '+this+'</button>';
	    	});
	    	html = html + '</div>';
    	}
//    	console.log(html);
    	$(element).html(html);
    	
    }
};
    	//$(element).html(valueAccessor());
    	//initAudioPlayersOnPage();
		//initVideoPlayersOnPage();
    	/*var html = '<div id="myCanvasContainer"><canvas width="200" id="myCanvas" heigth="160"><p>Your browser doesn\'t support Canvas</p><ul>';
    	$.each(valueAccessor()(),function(){
    		console.log(this._id);
    		console.log(this.count);
    		html = html + '<li><a style="font-size: '+this.count+'pt" href="#" onclick="return mainVM.addFilterTag(\''+this._id+'\');">'+this._id+'</a></li>';
    	});
    	html = html + '</ul></canvas></div>';
    	console.log(html);
    	$(element).html(html);
    	$('#collapseTAGS').collapse('show');
    	TagCanvas.Start('myCanvas');

    	 if( ! $('#myCanvas').tagcanvas({
    	     textColour : '#000000',
    	     outlineThickness : 1,
    	     maxSpeed : 0.06,
    	     depth : 0.5,
    	     weightSizeMin : 15,
    	     weightSizeMax : 40,
    	     weightMode : "size",
    	     weight:true,
    	     initial : [0.05, 0.1]
    	     
    	   })) {
    	   console.log("asd");
    	     // TagCanvas failed to load
    	     $('#myCanvasContainer').hide();
    	   }
    	   
    	$('#myCanvas').width($('#myCanvas').parent().parent().parent().width()*0.9);
    	*/


var askForLocationsOnMap = function(){
	//request the marker info via AJAX for the current bounds of the map
//	console.log("askForLocationsOnMap");
	var bounds = map.getBounds();
	var minll = bounds.getSouthWest();
	var maxll=bounds.getNorthEast();
	var url = '/mapBox/'+minll.lng+'/'+minll.lat+'/'+maxll.lng+'/'+maxll.lat;
	$.getJSON(url, function(allData){
		if(allData != null && allData.res=="OK"){
			console.log(allData.data);
			selectedMarkers.clearLayers();
			mainVM.selectedPOIS(allData.data);
			if(allData.data != null){
				$.each(allData.data,function(){
					selectedMarkers.addLayer(new L.Marker(L.latLng(this.fields.geoloc.lat,this.fields.geoloc.lon),{icon: selectedMarker}).bindPopup(this.fields.geoloc.display_name+"<br><button type='button' class='btn btn-primary' onclick='mainVM.goToDocument(\""+this._id.$oid+"\"); clickBubble: false'><i class='fa fa-check'/></button>"));
				});
			}
		}else{
			console.log("map data could not be loaded");
			//showMessage("Project could not be selected", "danger");
		}
		
	});
}

function onMapMove(e) {
	askForLocationsOnMap();
}

function isChar(str) {
	  return /^[a-zA-Z()]+$/.test(str);
}


var changeLanguage = function(lang){
	$.get("lang/" + lang,function(data) {
//		console.log("language changed to " + lang);
//		console.log(data);
		// reload page - so that language change takes effect
		location.reload(true);
	});
	return false;
}

ko.validation.rules.pattern.message = 'Invalid.'; 

ko.validation.configure({
    registerExtenders: true,
    messagesOnModified: true,
    insertMessages: true,
    parseInputAttributes: true,
    messageTemplate: null
});

var showMessage = function(msg, type){
	// possible types: success, info, warning, danger
	type = typeof type == 'undefined' ? 'alert alert-success alert-dismissable' : 'alert alert-'+type+' alert-dismissable';
//	console.log("set message");
//	console.log(msg);
	message(msg);
	messageShown(true);
	messageClass(type);
} 

var deleteMessage = function() {
	self.message("");
	self.messageShown(false);
}

var initImageUpload =function(uploaderID,progressBarID,targetID){
	var acceptedFileTypes = /(\.|\/)(gif|jpe?g|png)$/i;
	$('#'+uploaderID).fileupload({
		acceptFileTypes: acceptedFileTypes,
		url: "/img/avatar",
		done: function(e, data){
			// console.log("Upload DONE");
			// console.log(data.result);
			if(data.result.res=="KO"){
				console.log(data.result.error);
			}
			// console.log(data.result.data['$oid']);
			setTimeout(function(){
				$('#'+targetID).val(data.result.data['$oid']).change();		
		    },200);
		},
		progress: function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            // console.log(progress+ " progress!");
 	       $('#'+progressBarID+' .progress-bar').css(
                'width',
                progress + '%'
            );
        }
	}).on('fileuploadprocessalways', function (e, data) {
	    var currentFile = data.files[data.index];
	    if (data.files.error && currentFile.error) {
	      // there was an error, do something about it
	      console.log(currentFile.error);
	    }
	  }).prop('disabled', !$.support.fileInput)
    .parent().addClass($.support.fileInput ? undefined : 'disabled');
	return "";
}

// Functions used to initialize and control the Leaflet Map
var map = null;
var maps = [];
var tempMarkers = null;
var selectedMarkers = null;
var tempMarker = L.AwesomeMarkers.icon({
	prefix: 'fa',
	icon: 'question',
	markerColor: 'blue'
});
var selectedMarker = L.AwesomeMarkers.icon({
	prefix: 'fa',
	icon: 'star',
	markerColor: 'green',
	spin: true 
}); 
var userMarker = L.AwesomeMarkers.icon({
	prefix: 'fa',
	icon: 'user',
	markerColor: 'orange'
});

/**
 * initializes the map at the div with the given id, centered at the point
 * provided adds a layer with openstreetmap tiles
 */
function initMap(id, lat, lng, zoom){
	id = typeof id !== 'undefined' ? id : 'map';
	lat = typeof lat !== 'undefined' ? lat : 47.0786521;
	lng = typeof lng !== 'undefined' ? lng : 15.4070155;
	zoom = typeof zoom !== 'undefined' ? zoom : 13;
	
	// check if map div exists
	if($('#'+id).length > 0){
		// check if already initialized
		if($('#map').hasClass('leaflet-container')){
//			console.log("map is already initialized!");
		}else{
			console.log("init map");
			map = L.map(id, {
				center: [lat, lng],
				zoom: zoom
			});
			
			var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
			var osmAttrib='Map data © OpenStreetMap contributors';

			L.tileLayer(osmUrl, {
				minZoom: 2,
			    attribution: osmAttrib,
			    maxZoom: 18
			}).addTo(map);

			tempMarkers = new L.MarkerClusterGroup();
			selectedMarkers = new L.MarkerClusterGroup();
			map.addLayer(tempMarkers);
			map.addLayer(selectedMarkers);
			
		}
		return true;
	}else{
		return false;	// no map initialized
	}
}

/**
 * Set Center View of Map
 */
function centerMapView(lat, lng, zoom){
//	console.log("center Map view!");
	zoom = typeof zoom !== 'undefined' ? zoom : 13;
	map.setView([lat, lng], zoom);
}

function centerMapViewCurrentPosition(){
	if (navigator.geolocation){
		navigator.geolocation.getCurrentPosition(showPositionOnMap);
    }else{
    	showMessage("Geolocation is not supported by this browser.",warning);
  	}
}
	  

function showPositionOnMap(position) {
	console.log("Latitude: " + position.coords.latitude + " Longitude: "
			+ position.coords.longitude);
	centerMapView(position.coords.latitude, position.coords.longitude, 16);
	tempMarkers.clearLayers();
	tempMarkers.addLayer(new L.Marker(L.latLng(position.coords.latitude,position.coords.longitude),{icon: userMarker}).bindPopup("Your Position"));
}


/**
 * Set View to bounds
 */
function fitMapToBounds(south, west, north, east){
	map.fitBounds([
		[south, west],	// Southwest
		[north, east]	// NorthEast
	]);
}


var youtubeUrlToId = function (url) {
	var regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]*).*/;
	var match = url.match(regExp);
	if (match&&match[2].length==11){
	    return match[2];
	}else{
	    return "";// error
	}
}

var timeStampToDate = function (timestamp, locale) {
//	console.log("TimestampToDate");
//	console.log(timestamp);
//	console.log(locale);
	/*
	 * var locale = $.cookie("locale"); if(locale == null){ locale = "en"; }
	 */
	var date = new Date(timestamp);
	var returnvalue="";
	switch (locale){
		case "de":
			returnvalue = date.getDate()+"."+date.getMonth()+"."+date.getFullYear()+", "+date.getHours()+":"+date.getMinutes();
			break;
		case "en":
			returnvalue = date.toString();
			break;
		default:
			returnvalue = date.toString();
	}
	return returnvalue;
}

function getAudioplayerHTML(index){
	return "<div id='jquery_jplayer_"+index+"' class=\"jp-jplayer\"></div><div id='jp_interface_"+index+"' class=\"jp-audio\"><div class=\"jp-type-single\"><div class=\"jp-gui jp-interface\"><ul class=\"jp-controls\"><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-play\" tabindex=\"1\">play</a></li><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-pause\" tabindex=\"1\">pause</a></li><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-stop\" tabindex=\"1\">stop</a></li><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-mute\" tabindex=\"1\" title=\"mute\">mute</a></li><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-unmute\" tabindex=\"1\" title=\"unmute\">unmute</a></li><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-volume-max\" tabindex=\"1\" title=\"max volume\">max volume</a></li></ul><div class=\"jp-progress\"><div class=\"jp-seek-bar\"><div class=\"jp-play-bar\"></div></div></div><div class=\"jp-volume-bar\"><div class=\"jp-volume-bar-value\"></div></div><div class=\"jp-time-holder\"><div class=\"jp-current-time\"></div><div class=\"jp-duration\"></div><ul class=\"jp-toggles\"><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-repeat\" tabindex=\"1\" title=\"repeat\">repeat</a></li><li class=\"jp-noclick\"><a href=\"javascript:;\" class=\"jp-repeat-off\" tabindex=\"1\" title=\"repeat off\">repeat off</a></li></ul></div></div><div class=\"jp-no-solution\"><span>Update Required</span>To play the media you will need to either update your browser to a recent version or update your <a href=\"http://get.adobe.com/flashplayer/\" target=\"_blank\">Flash plugin</a>.</div></div></div>";
}

function getVideoplayerHTML(index){
    return "<div id='jp_container_"+index+"' class=\"jp-video \"><div class=\"jp-type-single\"><div id=\"jquery_jplayer_v_"+index+"\" class=\"jp-jplayer\"></div><div class=\"jp-gui\"><div class=\"jp-video-play\"><a href=\"javascript:;\" class=\"jp-video-play-icon\" tabindex=\"1\">play</a></div><div class=\"jp-interface\"><div class=\"jp-progress\"><div class=\"jp-seek-bar\"><div class=\"jp-play-bar\"></div></div></div><div class=\"jp-current-time\"></div><div class=\"jp-duration\"></div><div class=\"jp-controls-holder\"><ul class=\"jp-controls\"><li><a href=\"javascript:;\" class=\"jp-play\" tabindex=\"1\">play</a></li><li><a href=\"javascript:;\" class=\"jp-pause\" tabindex=\"1\">pause</a></li><li><a href=\"javascript:;\" class=\"jp-stop\" tabindex=\"1\">stop</a></li><li><a href=\"javascript:;\" class=\"jp-mute\" tabindex=\"1\" title=\"mute\">mute</a></li><li><a href=\"javascript:;\" class=\"jp-unmute\" tabindex=\"1\" title=\"unmute\">unmute</a></li><li><a href=\"javascript:;\" class=\"jp-volume-max\" tabindex=\"1\" title=\"max volume\">max volume</a></li></ul><div class=\"jp-volume-bar\"><div class=\"jp-volume-bar-value\"></div></div><ul class=\"jp-toggles\"><li><a href=\"javascript:;\" class=\"jp-full-screen\" tabindex=\"1\" title=\"full screen\">full screen</a></li><li><a href=\"javascript:;\" class=\"jp-restore-screen\" tabindex=\"1\" title=\"restore screen\">restore screen</a></li><li><a href=\"javascript:;\" class=\"jp-repeat\" tabindex=\"1\" title=\"repeat\">repeat</a></li><li><a href=\"javascript:;\" class=\"jp-repeat-off\" tabindex=\"1\" title=\"repeat off\">repeat off</a></li></ul></div></div></div><div class=\"jp-no-solution\"><span>Update Required</span>To play the media you will need to either update your browser to a recent version or update your <a href=\"http://get.adobe.com/flashplayer/\" target=\"_blank\">Flash plugin</a>.</div></div></div>";
}

function getYoutubeHTML(videoID){
    return "<div class=\"flex-video widescreen\" style=\"margin: 0 auto;text-align:center;\"><iframe src=\"//www.youtube.com/embed/"+videoID+"?rel=0\" frameborder=\"0\" allowfullscreen></iframe></div>";
}

function init_js_audioPlayer(file,location) {
//	console.log("init audio player: "+file+" "+location);
	jQuery("#jquery_jplayer_" + location).jPlayer( {
		ready: function () {
			$(this).jPlayer("setMedia", {
		mp3: "/audio/"+file
		  });
		},
		play: function() {
			$(this).jPlayer("pauseOthers");
		},
		cssSelectorAncestor: "#jp_interface_" + location,
		swfPath: "/assets/swf"
		// solution:"flash",
	  });
	return;
}

function init_js_videoPlayer(file,location) {
//	console.log("init videoplayer");
//	console.log(file);
//	console.log(location);
	jQuery("#jquery_jplayer_v_" + location).jPlayer( {
		ready: function () {
			$(this).jPlayer("setMedia", {
		m4v: "/video/"+file
		  });
		},
		play: function() {
			$(this).jPlayer("pauseOthers");
		},
		cssSelectorAncestor: "#jp_container_" + location,
		swfPath: "/assets/swf",
		supplied: "m4v"
		// solution:"flash",
	});
	return;
}

function initAudioPlayersOnPage(){
	// init all audioplayer divs on page
	// count existing players
	var existing= $('[id^=jp_interface_]').length;
	$("[type='audioplayer']").each( function( key, value ) {
//	    console.log( key + ": " + $(value).html() );
	    var audioFile = $(value).html();
	    $(value).attr('type','');	//remove attribute type
	    if(audioFile!=""){
	        // create html audioplayer
	        $(value).html(getAudioplayerHTML(key+1+existing));
	        // initaudioplayer
	        init_js_audioPlayer(audioFile, (key+1+existing));
	    }else{	//hide div
	    	$(value).parent().parent().parent().addClass("hidden");
	    }
	});
	return "";
}

function initImagesOnPage(){
	// init all audioplayer divs on page
	// count existing players
	$("[type='genCMSimage']").each( function( key, value ) {
	    var src = $(value).attr('src');
	    $(value).attr('type','');	//remove attribute type
	    if( src != "" && src != "img/"){
//	    	console.log("remove hidden from: ");
	    	$(value).removeClass("hidden");
	    }
	});
	return "";
}

function initVideoPlayersOnPage(){
	// init all videoplayer divs on page
	// count existing players - > offset to index
	var existing= $('[id^=jquery_jplayer_v_]').length;
//	console.log("existing players: "+existing);
	
	$("[type='videoplayer']").each( function( key, value ) {
//	    console.log( key + ": " + $(value).html() );
	    var videoFile = $(value).html();
	    $(value).attr('type','');	//remove attribute type
	    if(videoFile!=""){
	    	$(value).removeClass("hidden");
	    	if(videoFile.substring(0,8) === 'YOUTUBE:'){
	            // init youtube video
	            $(value).html(getYoutubeHTML(videoFile.substring(8)));
	        }else{
	            // create html videoplayer
	            $(value).html(getVideoplayerHTML(key+1+existing));
	            // initaudioplayer
	            init_js_videoPlayer(videoFile, (key+1+existing));
	       }
	    }else{	//hide div
	    	$(value).parent().parent().parent().addClass("hidden");
	    }
	});
	return "";
}

function initMapsOnPage(){
	$("[type='mapcontent']").each( function( key, value ) {
//	    console.log( key + ": " + $(value).html() );
	    var newID = "mapcontent_" + $('[id^="mapcontent_"]').length;
	    var values = $(value).html().split(genCMSseperator);
	    if(values.length == 4){
		    var lon = values[0];
		    var lat = values[1];
		    var display = values[2];
		    var heightMulti = values[3];
		    var zoom = 13;
//		    console.log(lon, lat, display, heightMulti, zoom);
		    $(value).html("");			//clean node
		    $(value).attr('type','');	//remove attribute type
		    $(value).attr('id', newID);	//set new ID
		    $(value).removeClass("hidden");
		    $(value).addClass("mapcontent");
		    //init map
//		    console.log("init map");
			var newMap = L.map(newID, {
				center: [lat, lon],
				zoom: zoom
			});
			//maps.push(newMap);
			var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
			var osmAttrib='© OpenStreetMap';

			L.tileLayer(osmUrl, {
				minZoom: 2,
			    attribution: osmAttrib,
			    maxZoom: 18
			}).addTo(newMap);
	    }else{	//hide div
	    	$(value).parent().parent().parent().addClass("hidden");
	    }
			newMap.addLayer( new L.Marker(L.latLng(lat,lon),{icon: selectedMarker}).bindPopup(display) );
	});
	return "";
}

message = ko.observable('');
messageShown = ko.observable(false);
messageClass = ko.observable('alert alert-success alert-dismissable');


var ProjectSelectVM = function(projects) {
	this.projects = ko.observable(projects);
	this.type="ProjectSelectVM";
	// TODO load templates of project
	this.selectProject = function(project){
		
//		console.log("project selected:");
//		console.log(ko.toJSON(project));
//		console.log(project._id.$oid());
		$.getJSON("project/select/" + project._id.$oid(), function(allData){
			if(allData.res=="OK"){	// Project successfully selected
//				console.log("Project was successfully selected");
				// TODO
				// Reload MenuHeader
				ko.cleanNode(document.getElementById("header"));
				$("#header").load("html/header",function() {
					headVM.init();
					ko.applyBindings(headVM, document.getElementById("header"));
//					console.log("header loaded!");
				});
			}else{
				console.log("project could not be selected");
				showMessage("Project could not be selected", "danger");
			}
			
		});
		// SET COOKIE
		// RELOAD CURRENT PAGE?
	}
}

var ProjectEditVM = function(selectedProject, orderedTags, styles) {
	var self = this;
	self.type="ProjectEditVM";
	self.localeOptions = ko.observableArray([ {"val":"de", "text":"German"}, 
	                                          {"val":"en", "text":"English"},
	                                          {"val":"es", "text":"Spanish"},
	                                          {"val":"fr", "text":"French"} ]);
	self.newTag = ko.observable("");
	self.newStyle = ko.observable("");
	self.newTagUnique=ko.observable(false);
	self.newTag.subscribe(function(updated){
//		console.log("new Tag updated!");
		self.newTagUnique( self.checkTagUniqueAndNew(self.newTag()) );
	});
	self.newAuthorRole = ko.observable("");
	self.newAuthorRoleUnique=ko.observable(false);
	self.newAuthorRole.subscribe(function(updated){
		console.log("new AuthorRole updated!");
		self.newAuthorRoleUnique( self.checkAuthorRoleUnique(self.newAuthorRole()) );
	});
	self.selectedProject = ko.observable(selectedProject);
	self.styles = ko.observableArray(styles);
	
	//self.distinctTags = ko.observableArray(distinctTags);
	self.orderedTags = ko.observableArray(orderedTags);
	
	
	self.init = function(){
//		console.log("init");
		$('ol.tagDrop').html(self.deserializeOrderedTags());
		$('ol.tagDrop').sortable2({
			group: 'genCMStags',
			onDragStart: function (item, container, _super) {
			    // Duplicate items of the no drop area
			    if(!container.options.drop)
			      item.clone().insertAfter(item);
			    _super(item);
			  },
			onDrop: function  (item, targetContainer, _super) {
			    if(item.parent().hasClass("level2")){
			    	item.children().remove();
			    	item.addClass("submenue");
			    	item.removeClass("mainmenue");
			    	item.append('<button type="button" class="removeTagFromStructure tagremove close"><i class="fa fa-trash-o fa-sm text-danger"/></button>');
			    }else{
			    	if(item.hasClass("submenue")){
			    		item.children().remove();
					    item.append('<ol class="level2"></ol>');
					    item.removeClass("submenue");
					    item.addClass("mainmenue");
			    	}else if(item.hasClass("mainmenue")){
			    		//move with childs
			    		//no change
			    	}else{ //no main menue entry
			    		item.children().remove();
			    		item.addClass("mainmenue");
			    		item.append('<button type="button" class="removeTagFromStructure tagremove close"><i class="fa fa-trash-o fa-sm text-danger"/></button>');
					    item.append('<ol class="level2"></ol>');
			    	}
			    	
			    }
			    //item.append('<button type="button" class="removeTagFromStructure tagremove close btn-danger"><i class="fa fa-trash-o fa-sm"/></button>');
			    //item.append('<ol class="level2"></ol>');
		        var clonedItem = $('<li/>').css({height: 0});
		        item.before(clonedItem);
			    clonedItem.animate({'height': item.height()});
			    
			    item.animate(clonedItem.position(), function  () {
			      clonedItem.detach();
			      _super(item);
			    });
		    }
		});

		$("ol.tagDrag").sortable2({
			group: 'genCMStags',
			drop: false,
			nested: true
		});
		
		$(document).on("click",'.removeTagFromStructure',function() {
//		    console.log($(this).parent());
		    $(this).parent().remove();
		});
	}
	
	/**
	 * Open the document
	 */
	self.openDoc = function(docID) {
			location.hash="doc/"+docID;
	}
	
	self.updateProjectBasicSettings = function(){
// console.log("updateProjectBasicSettings");
// console.log(ko.mapping.toJSON(self.selectedProject));
		$.ajax({
			url : "/project/basicSettings",
			type : "POST",
			dataType : "xml/html/script/json", // expected format
			contentType : "application/json", // send as JSON
			data : ko.mapping.toJSON(this.selectedProject),
			complete : function(returnedData) {
				console.log("Returned: " + returnedData);
				if (returnedData.status == 200) {
					showMessage('Project basic Settings successfully saved!', "success");
					// alert("save successfull");
					/*
					 * if (saveUrl == "/new/doctype") { // new document // -
					 * load new Data from Server! location.hash = "editDocType" +
					 * "/" + JSON.parse(returnedData.responseText).data._id; }
					 * self.message("Document Type successfully saved");
					 * self.messageShown(true);
					 */
				}else{
					showMessage("Project Settings could not be saved", "danger");
					console.log(returnedData);
				}
			}
		});
	}
	
	//adds a tag to the drag list - tags will only be stored if they are in the drop list
//	self.addTagNEW = function(){
//		if (self.checkTagUniqueAndNew(self.newTag()) ){
//			self.distinctTags.push(self.newTag());
//			self.distinctTags.sort();
//			/*var url = "/project/tag/add/"+encodeURIComponent(self.newTag());
//			$.getJSON(url, function(allData) {
//				if(allData.res="OK"){
//					self.selectedProject().tags.push(self.newTag());
//					self.newTag("");
//				}else{
//					showMessage("Project Tag could not be added", "danger");
//				}
//			}).fail(function( response, textStatus, error ) {
//				console.log( "Request Failed: " + textStatus + ", " + error );
//				if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
//					cleanMainNode();
//					loadForbiddenPageMainNode();
//				}
//			});*/
//		}else{
//			console.log("Tag already exists");
//		}
//	}
	
	self.addStyle = function(){
		if (self.newStyle()!=""){
			console.log("addStyle")
			var url = "/new/doctype/style/"+encodeURIComponent(self.newStyle());
			$.getJSON(url, function(allData) {
				if(allData.res="OK"){
					self.styles.push(allData.data.style);
				}else{
					showMessage("Project Style could not be created", "danger");
				}
			}).fail(function( response, textStatus, error ) {
				console.log( "Request Failed: " + textStatus + ", " + error );
				if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
					cleanMainNode();
					loadForbiddenPageMainNode();
				}
			});
		}
	}
	
	self.deleteStyle = function(style){
		var url = "/delete/doctype/style/"+style._id.$oid;
		$.getJSON(url, function(allData) {
			if(allData.res="OK"){
				self.styles.remove(style);
			}else{
				showMessage("Project Style could not be deleted", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.saveStyle = function(style){
		console.log(style);
		var url = "/update/doctype/style"; 
		$.ajax({
			url : url,
			type : "POST",
			dataType : "json", // expected format
			contentType : "application/json", // send as JSON
			data : JSON.stringify(style),
			complete : function(returnedData) {
				console.log(returnedData);
				if (returnedData.status == 200) {
					showMessage("Style saved", "success");
				}else{
					showMessage("Style could not be saved", "danger");
				}
			}
		});
	}
	
	//adds a tag to the drag list
	self.addTag = function(){
		if (self.checkTagUniqueAndNew(self.newTag()) ){
			var url = "/project/tag/add/"+encodeURIComponent(self.newTag());
			$.getJSON(url, function(allData) {
				if(allData.res="OK"){
					self.selectedProject().tags.push(self.newTag());
					self.selectedProject().tags.sort();
				}else{
					showMessage("Project Tag could not be added", "danger");
				}
			}).fail(function( response, textStatus, error ) {
				console.log( "Request Failed: " + textStatus + ", " + error );
				if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
					cleanMainNode();
					loadForbiddenPageMainNode();
				}
			});
		}else{
			console.log("Tag already exists");
		}
	}
	
	self.saveTags = function(){
//		console.log("saving");
		data = {"tags":self.serializeOrderedTags()};
		var url = "/project/saveTags"; 
		$.ajax({
			url : url,
			type : "POST",
			dataType : "json", // expected format
			contentType : "application/json", // send as JSON
			data : JSON.stringify(data),
			complete : function(returnedData) {
				console.log(returnedData);
				if (returnedData.status == 200) {
					showMessage("Menue saved", "success");
				}else{
					showMessage("Menue could not be saved", "danger");
				}
			}
		});
	}
	
	self.serializeOrderedTags = function(){
		var orderedTags = new Array();
		var outerOrder = 1;
		$('ol.tagDrop').children().each(function(){
		
			var children = $(this).children("ol").children("li");
			var parentName = $(this).attr('tagname');//$(this).contents().filter(function() {return this.nodeType == 3;}).text();
			orderedTags.push({"name":parentName, "parent": null, "sort":outerOrder});
			outerOrder++;
			var childNames = new Array();
//			console.log(children,parentName);
			var innerOrder = 1;
			$(children).each(function(){
				var childName = $(this).attr('tagname');//$(this).text();
				orderedTags.push({"name":childName, "parent": parentName, "sort":innerOrder});
				innerOrder++;
			});
		});
		return orderedTags;
	}
	
	self.deserializeOrderedTags = function(){
		var orderedTagsHTML = "";
		$.each(self.orderedTags(), function(){
			orderedTagsHTML += "<li class='level1 mainmenue' tagname='"+this.tag+"'>"+this.tag;
			orderedTagsHTML += '<button type="button" class="removeTagFromStructure tagremove close"><i class="fa fa-trash-o fa-sm text-danger"/></button>';
			orderedTagsHTML += "<ol class='level2'>";
			if(this.childs != null && this.childs.length>0){
				$.each(this.childs,function(){
					if(this != ""){
						console.log("child");
						console.log(this);
						orderedTagsHTML += "<li class='level1 submenue' tagname='"+this+"'>"+this;
						orderedTagsHTML += '<button type="button" class="removeTagFromStructure tagremove close"><i class="fa fa-trash-o fa-sm text-danger"/></button>'+"</li>";
					}
				});
			}
			orderedTagsHTML += "</ol></li>";
		});
		return orderedTagsHTML;
	}
	
	self.removeTag = function(tag){
		var url = "/project/tag/remove/"+encodeURIComponent(tag);
		$.getJSON(url, function(allData) {
			if(allData.res="OK"){
				self.selectedProject().tags.remove(tag);
			}else{
				showMessage("Project Tag could not be removed", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.addAuthorRole = function(){
		if (self.checkAuthorRoleUnique(self.newAuthorRole()) ){
			var url = "/project/authorrole/add/"+encodeURIComponent(self.newAuthorRole());
			$.getJSON(url, function(allData) {
				if(allData.res="OK"){
					self.selectedProject().authorroles.push({name:ko.observable(self.newAuthorRole()), jobdoc:ko.observable("")});
					self.newAuthorRole("");
				}else{
					showMessage("Author Role could not be added", "danger");
				}
			}).fail(function( response, textStatus, error ) {
				console.log( "Request Failed: " + textStatus + ", " + error );
				if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
					cleanMainNode();
					loadForbiddenPageMainNode();
				}
			});
		}else{
			console.log("Tag already exists");
		}
	}
	
	self.removeAuthorRole = function(role){
		var url = "/project/authorrole/remove/"+encodeURIComponent(role.name());
		$.getJSON(url, function(allData) {
			if(allData.res="OK"){	
				self.selectedProject().authorroles.remove(role);
			}else{
				showMessage("Author Role could not be deleted<br>"+allData.error, "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.checkTagUniqueAndNew = function(tag){
//		console.log("check tag "+ tag);
//		console.log(self.selectedProject().tags());
		if(self.selectedProject().tags.indexOf(tag) < 0) {
			return true;
		}else{
			return false;
		}
	}
	
	self.checkAuthorRoleUnique = function(role){
//		console.log("check authorRole "+ role);
//		console.log(self.selectedProject().authorroles());
		var match = ko.utils.arrayFirst(self.selectedProject().authorroles(), function(item) {
			return role === item.name;
		});
		if(match){
			return false;
		}else{
			return true;
		}
	}
}

var ProjectNewVM = function() {
	this.newProject = ko.observable({});
	this.type="ProjectNewVM";
	// Custom Validation Rules
	ko.validation.rules['uniqueProjectTitle'] = {
		async: true,
		validator : function(val, parms, callback) {
//			console.log("uniqueValidation called!!: " + val);
//			console.log(parms);
			if(val.length<2){
				return true;
			}else{
				// call server!!!!
				$.getJSON("/project/checktitle/"+val, function(allData) {
					console.log("title checked:");
					console.log(allData.unique);
					callback(allData.unique);
				});
				
			}
		},
		message : 'This title is used by another project.'
	};
	
	ko.validation.registerExtenders();
	
	// Add Validation
	this.newProject().title = ko.observable("").extend({ required:true, minLength: 6, maxLength: 20, uniqueProjectTitle:23});
	// this.newProject().description = ko.observable("");
	// Error Object for validation
	this.validationErrors = ko.validation.group([this.newProject().title]);
	
	
	// TODO
	this.saveProject = function() {
//		console.log("saveProject");
//		console.log(this.newProject());
//		console.log("validationErrors: ");
//		console.log(this.validationErrors());
//		console.log("validationErrors: ");
//		console.log(this.validationErrors().length);
		// (ko.mapping.toJSON(self.selectedDocType(), null, 2));
		if(this.validationErrors().length == 0){	// Validation OK - Save
			
			if (typeof this.newProject()._id === 'undefined') {
				var saveUrl = "/new/project";	// create Project
			} else {
				var saveUrl = "/project/" + this.newProject()._id.$oid();	// update
																			// project
			}
//			console.log("save project:");
//			console.log(ko.mapping.toJSON(this.newProject()));
			$.ajax({
				url : saveUrl,
				type : "POST",
				dataType : "xml/html/script/json", // expected format
				contentType : "text/plain", // send as JSON
				data : this.newProject().title(),
				complete : function(returnedData) {
//					console.log(returnedData);
					if (returnedData.status == 200) {
						showMessage('Project successfully created!');
//						console.log(returnedData);
						// alert("save successfull");
						/*
						 * if (saveUrl == "/new/doctype") { // new document // -
						 * load new Data from Server! location.hash =
						 * "editDocType" + "/" +
						 * JSON.parse(returnedData.responseText).data._id; }
						 * self.message("Document Type successfully saved");
						 * self.messageShown(true);
						 */
					}
				}
			});
		} else {	// Validation NOK
			var errorMsg = 'Project could not be created - Please Check Validation Messages!';
			this.validationErrors.showAllMessages();
			showMessage(errorMsg,'danger');
		}
	}
}

var HeaderVM = function(){
	var self= this;
	self.currentLocale = "en";
	self.initialized = false;
	self.currentPage = ko.observable(0);
	self.availablePages = ko.observable(0);
	self.projectTitles = null;
	self.projectRoles = null;
	self.projectTags = null;
	self.loggedInUser = ko.observable({});
	self.selectedProject = ko.observable("");
	self.selectedProjectEditorial = ko.observable("");
	self.selectedProjectDD = ko.observable("");
	self.selectedAuthorRoleDD = ko.observable("");
	self.tagsStructure = ko.observableArray([]);
	self.projectConnectionTemplates = null;
	self.selectedTag = ko.observable("");
	self.labelLocales = ko.observable("");
	self.filterType = ko.observable("");
	self.availableTypes = ko.observableArray([]);
	self.filterTags = ko.observableArray([]);
	self.availableTags = ko.observable("");
	
//	console.log("new header vm");
	self.type="HeaderVM";
	
	self.goToMapSearch = function(){
		console.log("open map search!");
		location.hash="map";
	}
	
	self.loadHeaderInfos = function(){
		$.getJSON("/headerInfos", function(allData) {
			if(allData.res=="OK"){
				
//				console.log("set project titles!");
				self.projectTitles=allData.data.titles;
//				console.log("set project roles");
				self.projectRoles=allData.data.roles;
//				console.log("set project tags");
				self.projectTags=allData.data.tags;
//				console.log("set user");
				self.loggedInUser(allData.data.user);
//				console.log("set project");
				self.selectedProject(allData.data.projectID);
//				console.log("set project editorial");
				self.selectedProjectEditorial(allData.data.editorial);
//				console.log("set templates!");
				self.projectConnectionTemplates = new Object();
//				console.log("set structure tags");
				self.tagsStructure(allData.data.navigationStructure == null ? new Array() : allData.data.navigationStructure);
//				console.log("set locale");
				self.currentLocale=allData.data.locale;
//				console.log("set locales for labels");
				self.labelLocales(allData.data.localeLabels);
				self.loadAvailableTags();
				$.get("/projectstyle",function(allData) {
					$('#customProjectStyle').text(allData);
				});
				var templates = allData.data.templates;
				if(templates != null){
					$.each(templates, function(key, value){
//						console.log(key + " : "+ value);
						self.projectConnectionTemplates[key] = {
							// precompile the templates for later usage
							"full": Handlebars.compile(value.full),
							"list": Handlebars.compile(value.list),
							"docType" : value.docType
						}
					});
				}
				self.initialized=true;
				if((location.hash === "" || location.hash === "#selectProject")  && self.selectedProject()!=""){
//					console.log("go to index");
					location.hash = "index"; // go to index page
				}else if(location.hash === "" & self.selectedProject()===""){
//					console.log("go to selectProject");
					location.hash = "selectProject"; // go to index page
				}
			}
		});
	}
	
	self.getProjectTitle = function(id){
		var title = self.projectTitles[id];
		if(typeof title === 'undefined'){
			return id;
		}else{
			return title;
		}
	}
	
	/**
	 * returns the author roles, which can be assigned to authors roles can only
	 * be assigned if there is a job description document linked!
	 */
	self.getProjectAuthorRoles = function(projectID, withEmpty){
		var roles = self.projectRoles[projectID];
		var rolesRet = new Array();
		if(typeof roles === 'undefined'){
			self.selectedAuthorRoleDD("");
			return rolesRet;	// empty Array
		}else{
			// take only roles with assigned job description document
			$.each(roles, function(){
				if(this.jobdoc != "" || withEmpty==true){
					rolesRet.push(this);
				}
			});
			if(typeof rolesRet[0] === 'undefined'){
				self.selectedAuthorRoleDD("");
			}else{
				self.selectedAuthorRoleDD(rolesRet[0].name);
			}
			return rolesRet;
		}
	}
	
	/**
	 * returns the author roles job description document - if any
	 */
	self.getProjectAuthorRoleDocument = function(roleName){
		if(self.selectedProject()==""){
			return "";
		}
		var roles = self.projectRoles[self.selectedProject()];
		var roleDoc = new Array();
		if(typeof roles === 'undefined'){
			return "";	// no role - no doc
		}else{
			// search for role
			$.each(roles, function(){
				if(this.name === roleName){
					roleDoc = this.jobdoc;
					return false;
				}
			});
			
			return roleDoc;
		}
	}
	
	/**
	 * returns the tags, which can be assigned to documents by authors
	 */
	self.getProjectTags = function(projectID){
		return self.projectTags[projectID];
	}
	
	/**
	 * returns an array of the available projecttitles where the user is
	 * projectadmin or if he or she is admin all
	 */
	self.getProjectTitleArrayAdmin = function(id){
		var selectCurrentPr = false;
		var ret = new Array();
		if(self.loggedInUser()==""){
			return ret;
		}
		if(self.loggedInUser().admin){	// all
//			console.log("admin");
			selectCurrentPr=true;
			$.each(self.projectTitles,function(elem,key){
				ret.push({
					id: elem,
					title: key
				});
			});
		}else{	// just with admin rights
//			console.log("projectadmin");
			$.each(self.loggedInUser().projectadmin,function(){
				if(this.projectID==self.selectedProject()){
					selectCurrentPr=true;
				}
				ret.push({
					id: this.projectID,
					title: self.getProjectTitle(this.projectID)
				});
			});
		}
		if(selectCurrentPr){
			self.selectedProjectDD(self.selectedProject());
		}else{
			self.selectedProjectDD(null);
		}
		return ret;
	}
	
	self.goToIndex = function(){
		location.hash="index";
	}
	
	self.loadIndexPage = function(){
		if(mainVM.type != 'UserViewVM'){
			self.goToIndex();
		}else{
			mainVM.loadIndexPage(0, true, false);
		}
	}
	
	self.goToNewProject = function(){
		location.hash = "newProject";
	}
	
	self.goToEditProject = function(){
		location.hash = "editProject";
	}
	
	self.goToSelectProject = function(){
		location.hash = "selectProject";
	}
	
	self.goToCreateDocument = function() {
		deleteMessage();

		location.hash = "createDocument";
	};
	
	self.goToDocTypesList = function() {
		deleteMessage();
		location.hash = "docTypes";
	};
	
	self.goToMyDocumentList = function(){
		deleteMessage();
		location.hash = "myDocuments";
	}
	
	self.goToUnreleasedDocuments = function(){
		deleteMessage();
		location.hash = "unreleasedDocuments";
	}
	
	self.goToManageUsers = function(){
		deleteMessage();
		location.hash = "users";
	}
	
	/*
	 * checks if the logged in user is admin for the current project
	 */
	self.isCurrentUserProjectAdmin = function(){
		var admin = false;
		if(self.loggedInUser()==""){
			return false;
		}
		if(self.loggedInUser().admin){	//admin is always project admin
			return true;
		}
		if(self.loggedInUser().projectadmin){
			$.each(self.loggedInUser().projectadmin, function(){
				if(this.projectID == self.selectedProject()){
					admin=true;
					return false;
				}
			});
		}
		return admin;
	}
	
	/*
	 * checks if the logged in user is author and returns the role for the current project
	 */
	self.isCurrentUserProjectAuthor = function(){
		var role = "";
		if(self.loggedInUser()==""){
			return role;
		}
		if(self.loggedInUser().author){
		$.each(self.loggedInUser().author, function(){
			if(this.projectID == self.selectedProject()){
				role=this.role;
				return false;
			}
		});
		}else{
			return "";
		}
		return role;
	}
	
	/**
	 * Returns the projectRoles of the logged in User for a specified project
	 */
	self.getProjectRolesCurrentUser = function(projectID){
		var roles="";
		if(self.loggedInUser().admin){
			roles = "Admin";
		}
		$.each(self.loggedInUser().projectadmin,function(){
			if(this.projectID == projectID){
				if(roles==""){
					roles="Projectadmin";
				}else{
					roles=roles+" | "+"Projectadmin";
				}
				return false;
			}
		});
		$.each(self.loggedInUser().author, function(){
			if(this.projectID == projectID){
				if(roles==""){
					roles="Author ("+this.role+")";
				}else{
					roles=roles+" | "+"Author ("+this.role+")";
				}
				return false;
			}
		});
		return roles;
	}
	
	self.isCurrentUserAdmin = function(){
		if(self.loggedInUser()==""){
			return false;
		}else{
			return self.loggedInUser().admin;
		}
		
	}
	
	/**
	 * checks if the logged in user is projectAdmin for some project
	 */
	self.isCurrentUserSomeProjectAdmin = function(){
		if(self.loggedInUser()==""){
			return false;
		}
		if(self.loggedInUser().admin){	//admin is always project admin
			return true;
		}
		return (self.loggedInUser().projectadmin.length > 0);
	}
	
	self.isCurrentUserProjectAdminFor = function(projectID){
		var admin = false;
		if(self.loggedInUser()==""){
			return false;
		}
		if(self.loggedInUser().admin){	//admin is always project admin
			return true;
		}
		$.each(self.loggedInUser().projectadmin, function(){
			if(this.projectID == projectID){
				admin=true;
				return false;
			}
		});
		return admin;
	}
	
	/**
	 * load the available document type connections with count - filtered by tags clicked
	 */
	self.loadAvailableTypes = function(){
		url = "/project/connections";
		var postData = {
				"tags": ko.mapping.toJS(self.filterTags),
				"connectionID" : ""
				}
		$.ajax({
			url : url,
			type : "POST",
			dataType : "json", // expected format
			contentType : "application/json", // send as JSON
			data : JSON.stringify(postData),
			complete : function(returnedData) {
//				console.log(returnedData);
				if (returnedData.status == 200 && returnedData.responseJSON.res==="OK") { // List of available DocumentType Connections loaded
					self.availableTypes(returnedData.responseJSON.data.connections);
				}else{
					console.log("Some Error occured");
					console.log(allData);
					//showMessage("Page could not be loaded", "danger");
				}
			}
		});
	}
	
	self.setFilterType = function(data){
//		console.log(data);
		self.filterType(data._id);
		self.loadAvailableTags();
		if(self.filterType()===""){ //unfiltered - show editorial
			self.loadIndexPage();
		}else{ //filtered - show no editorial
			self.loadIndexPage();
		}
	}
	
	/**
	 * load the available tags with count - filtered by tags clicked & connection
	 */
	self.loadAvailableTags = function(){
		url = "/project/tags";
		var postData = {
				"tags": ko.mapping.toJS(self.filterTags),
				"connectionID" : self.filterType()
				}
		$.ajax({
			url : url,
			type : "POST",
			dataType : "json", // expected format
			contentType : "application/json", // send as JSON
			data : JSON.stringify(postData),
			complete : function(returnedData) {
//				console.log(returnedData);
				if (returnedData.status == 200 && returnedData.responseJSON.res==="OK") { // List of available DocumentType Connections loaded
					//self.availableTypes(returnedData.responseJSON.data.connections);
					self.availableTags(returnedData.responseJSON.data.tags);
					//self.refreshCountStructure();
					//self.availablePages(Math.ceil(returnedData.responseJSON.data.results/self.documentsPerPage));
					//self.documentsList(ko.mapping.fromJS(returnedData.responseJSON.data.documents)());
				}else{
					console.log("Some Error occured");
					//showMessage("Page could not be loaded", "danger");
				}
			}
		});
	}
	
	self.getTagCount = function(tag){
		var struct = new Array();
		var match = ko.utils.arrayFirst(headVM.availableTags(), function(item) {
			    //console.log(item);
			    return tag === item._id;
		});
		if(match === null){
			return 0;
		}else{
			return match.count;
		}
	}
	
	self.addFilterTag = function(event){
		console.log($(event.target).attr('tagName'));
		var tagName = $(event.target).attr('tagName');
		if(tagName && tagName != ""){
			if(self.filterTags.indexOf(tagName) == -1){ //value not found - insert & update docs
				self.filterTags.push(tagName);
				//load filtered documents / no editorial
				self.loadAvailableTags();
				self.loadAvailableTypes();
				if(mainVM.type === 'UserViewVM'){
					self.loadIndexPage();
				}else{
					self.goToIndex();
				}
			}
		}
	}
	
	self.removeFilterTag = function(event){
		var tagName = $(event.target).attr('tagName');
		if(tagName && tagName != ""){
			self.filterTags.remove(tagName);
			//load filtered documents / no editorial
			self.loadAvailableTags();
			self.loadAvailableTypes();
			if(mainVM.type === 'UserViewVM'){
				self.loadIndexPage();
			}else{
				self.goToIndex();
			}
		}
	}
	
	self.clearFilters = function(){
		self.filterTags([]);
		self.filterType("");
		self.loadAvailableTags();
		self.loadAvailableTypes();
		if(mainVM.type === 'UserViewVM'){
			self.loadIndexPage();
		}else{
			self.goToIndex();
		}
	}
	
	self.init = function(){
//		console.log("initheadcalled");
		// Load Information about logged in user and project id into header
		self.loadHeaderInfos();
//		$(document).on("click",'.tagNavigationLink',function() {
//		    console.log(this);
//		    console.log($(this).attr("tagName"));
//		    //$(this).parent().remove();
//		});
	}
	
	self.init();
	
}

var EditUsersVM = function(){
	var self = this;
	self.type="EditUsersVM";
	self.usersPerPage=10;
	self.users = ko.observableArray([]);
	self.selectedUser = ko.observable({});
	self.projectID = ko.observable("");
	self.currentPage = ko.observable(0);
	self.availablePages = ko.observable(0);
	
	self.sortAsc = ko.observable(true);
	self.sortBy = ko.observable("_id");
	self.searchProjectOnly = ko.observable(true);
	self.searchProjectOnly.subscribe(function(updated){
		self.loadUsersPage(0, false);
	});
	self.filterOpt = ko.observable('notFiltered');
	self.filterOpt.subscribe(function(updated) {
		self.loadUsersPage(0, false);
	});
	
	self.firstName = ko.observable("");
	self.lastName = ko.observable("");
	self.userID = ko.observable("");
	
	
	self.goToEditUser = function(user){
		deleteMessage();
		location.hash = "editUser/"+user.userid();
	}
	
	/**
	 * loads the docTypes from the server (with pagination) and loads the view
	 * if not already loaded
	 */
	self.loadUsersPage = function(page, withHTML){
		// load Data from Server
		self.currentPage(page);
		
		 // /documents/:perPage/:page/:projectOnly/:filteredOnly/:inEdit/:published/:deleted/:orderBy/:asc
			// controllers.DocumentController.getMyDocuments(page: Int, perPage:
			// Int, projectOnly: Boolean,filteredOnly:Boolean, inEdit: Boolean,
			// published: Boolean, deleted: Boolean, orderBy: String, asc:
			// Boolean)
		var filteredOnly = self.filterOpt()!='notFiltered';
		var admin = self.filterOpt()=='admin';
		var projectAdmin = self.filterOpt()=='projectAdmin';
		var author = self.filterOpt()=='author';
		
		var url = "/users/"+self.usersPerPage+"/"+self.currentPage()+"/"+self.searchProjectOnly()+"/"+filteredOnly+"/"+admin+"/"+projectAdmin+"/"+author+"/"+self.sortBy()+"/"+self.sortAsc()+"?firstName="+self.firstName()+"&lastName="+self.lastName()+"&userID="+self.userID();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				self.availablePages(Math.ceil(allData.data.results/self.usersPerPage));
				var observableData = ko.mapping.fromJS(allData.data.users);
				var array = observableData();
				self.users(array);
				self.projectID(allData.data.projectID);
				// load document list html from Server
				if(withHTML){
					cleanMainNode();
					$("#main").load("html/manageUsers",function() {
						applyBindingsMainNode(mainVM);
					});
				}
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.updateSelectedUser = function(){
		var saveUrl = "user";
		$.ajax({
			url : saveUrl, // "/user/" +
			type : "POST",
			dataType : "json/xml/html/script", // expected format
			contentType : "application/json", // send as JSON
			data : ko.mapping.toJSON(self.selectedUser()),
			complete : function(returnedData) {
//				console.log(returnedData);
				if (returnedData.status == 200) {
					// alert("save successfull");
					showMessage("Account successfully updated", "success"); 	// possible
																				// types:
																				// success,
																				// info,
																				// warning,
																				// danger("Document
																				// Type
																				// successfully
																				// saved");
				}else if(returnedData.status == 401 || returnedData.status == 403){
					showMessage("User could not be updated - "+JSON.parse(returnedData.responseText).error, "danger");
				}else{
					showMessage("User could not be updated", "danger");
				}
			}
		});
	}
	
	self.getProjectRoles = function(index){
		// console.log("isProjectAdmin");
		var roles="";
		if(self.users()[index()].admin()){
			roles = "Admin";
		}
		$.each(ko.mapping.toJS(self.users()[index()].projectadmin()),function(){
			if(this.projectID == self.projectID()){
				if(roles==""){
					roles="Projectadmin";
				}else{
					roles=roles+" | "+"Projectadmin";
				}
				return false;
			}
		});
		$.each(ko.mapping.toJS(self.users()[index()].author()), function(){
			if(this.projectID == self.projectID()){
				if(roles==""){
					roles="Author ("+this.role+")";
				}else{
					roles=roles+" | "+"Author ("+this.role+")";
				}
				return false;
			}
		});
		return roles;
	}
	
	/**
	 * add the currently selected user to admins or remove him
	 */
	self.turnAdmin = function(add){
		var url = (add ? "/set/admin/" : "/remove/admin/") + self.selectedUser().userid();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				self.selectedUser().admin(add);
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing admin setting!")
				showMessage("User admin setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				showMessage("User admin setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}
		});
	}
	
	/**
	 * add selected user as author to project
	 */
	self.addAuthor = function(project,role){
		var url = "/set/author/" + self.selectedUser().userid() +"/"+role+"/"+project;
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				// self.selectedUser().admin(add);
				console.log("reload user now");
				self.loadUser(self.selectedUser().userid(),false);
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing author setting!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}
		});		
	}
	
	
	/**
	 * remove selected user from project authors
	 */
	self.removeAuthor = function(project){
		var url = "/remove/author/" +  self.selectedUser().userid() +"/"+project;
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				// self.selectedUser().admin(add);
				console.log("reload user now");
				self.loadUser(self.selectedUser().userid(),false);
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing author setting!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}
		});		
	}
	
	/**
	 * add selected user to project admins
	 */
	self.addProjectAdmin = function(project,role){
		var url = "/set/projectadmin/" + self.selectedUser().userid()+"/"+project;
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				console.log("reload user now");
				self.loadUser(self.selectedUser().userid(),false);
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing author setting!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}
		});		
	}
	
	/**
	 * remove selected user from project admins
	 */
	self.removeProjectAdmin = function(project){
		var url = "/remove/projectadmin/" +  self.selectedUser().userid() +"/"+project;
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				console.log("reload user now");
				self.loadUser(self.selectedUser().userid(),false);
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized"){
				console.log("User is not authorized for editing author setting!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}else if(response.responseJSON.error=="Credentials required"){
				console.log("User is not logged in!")
				showMessage("User author setting could not be changed - "+JSON.parse(returnedData.responseText).error, "danger");
			}
		});		
	}
	
	/**
	 * load the data of a single user and the html of the user account settings
	 * page if needed
	 */
	self.loadUser = function(userid, withHTML){
		var url = "user/"+userid;
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				mainVM.selectedUser(ko.mapping.fromJS(allData.data.user));
				mainVM.projectID(allData.data.projectID);
				
				if(withHTML){
					// load document list html from Server
					cleanMainNode();
					$("#main").load("html/userSettings",function() {
						applyBindingsMainNode(mainVM);
					});
				}
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
		
	}
	
	/**
	 * Sorts the doc type list
	 */
	self.sortDocumentPage = function(sortBy){
		if(self.sortBy() == sortBy){	// Change Order
			self.sortAsc(!self.sortAsc());
		}else{							// Change Column
			self.sortBy(sortBy);
		}
		self.loadUsersPage(headVM.currentPage());
	}
}

var UnreleasedDocumentsVM = function(){
	var self = this;
	self.type="UnreleasedDocumentsVM";
	self.documentsPerPage=10;
	self.unreleasedDocuments = ko.observableArray([]);
	self.projectID = ko.observable("");
	self.currentPage = ko.observable(0);
	self.availablePages = ko.observable(0);
	
	self.sortAsc = ko.observable(true);
	self.sortBy = ko.observable("_id");
		
	/**
	 * loads the docTypes from the server (with pagination) and loads the view
	 * if not already loaded
	 */
	self.loadUnreleasedDocumentsPage = function(page, withHTML){	
		// load Data from Server
		headVM.currentPage(page);
		self.currentPage(page);
		
		var url = "/unreleaseddocuments/"+self.documentsPerPage+"/"+headVM.currentPage()+"/"+self.sortBy()+"/"+self.sortAsc();
		$.getJSON(url, function(allData) {
			if(allData.res=="OK"){
				headVM.availablePages(Math.ceil(allData.data.results/self.documentsPerPage));
				self.availablePages(headVM.availablePages());
				var observableData = ko.mapping.fromJS(allData.data.documents);
				var array = observableData();
				self.unreleasedDocuments(array);
				self.projectID(allData.data.projectID);
				if(withHTML){
					// load document list html from Server
					cleanMainNode();
					$("#main").load("html/documentListUnreleased",function() {
							applyBindingsMainNode(mainVM);
					});
				}
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.goToEditDocument = function(document){
		location.hash = "editDocument/"+document._id.$oid();
	}
	
	/**
	 * Sorts the doc type list
	 */
	self.sortDocumentPage = function(sortBy){
		if(self.sortBy() == sortBy){	// Change Order
			self.sortAsc(!self.sortAsc());
		}else{							// Change Column
			self.sortBy(sortBy);
		}
		self.loadMyDocumentsPage(headVM.currentPage(),false);
	}
}

var MyDocumentsVM = function(){
	var self = this;
	self.type="MyDocumentsVM";
	self.documentsPerPage=10;
	self.myDocuments = ko.observableArray([]);
	self.projectID = ko.observable("");
	self.currentPage = ko.observable(0);
	self.availablePages = ko.observable(0);
	
	self.sortAsc = ko.observable(false);
	self.sortBy = ko.observable("fields.modifiedAt");
	self.searchProjectOnly = ko.observable(true);
	self.searchProjectOnly.subscribe(function(updated){
		self.loadMyDocumentsPage(0,false);
	});
	self.filterOpt = ko.observable('notFiltered');
	self.filterOpt.subscribe(function(updated) {
		self.loadMyDocumentsPage(0,false);
	});
		
	/**
	 * loads the docTypes from the server (with pagination) and loads the view
	 * if not already loaded
	 */
	self.loadMyDocumentsPage = function(page, withHTML){	
		// load Data from Server
		headVM.currentPage(page);
		self.currentPage(page);
		
		 // /documents/:perPage/:page/:projectOnly/:filteredOnly/:inEdit/:published/:deleted/:orderBy/:asc
			// controllers.DocumentController.getMyDocuments(page: Int, perPage:
			// Int, projectOnly: Boolean,filteredOnly:Boolean, inEdit: Boolean,
			// published: Boolean, deleted: Boolean, orderBy: String, asc:
			// Boolean)
		var filteredOnly = self.filterOpt()!='notFiltered';
		var inEditOnly = self.filterOpt()=='inEdit';
		var publishedOnly = self.filterOpt()=='published';
		var deletedOnly = false;
		
		var url = "/documents/"+self.documentsPerPage+"/"+headVM.currentPage()+"/"+self.searchProjectOnly()+"/"+filteredOnly+"/"+inEditOnly+"/"+publishedOnly+"/"+deletedOnly+"/"+self.sortBy()+"/"+self.sortAsc();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				headVM.availablePages(Math.ceil(allData.data.results/self.documentsPerPage));
				self.availablePages(headVM.availablePages());
				var observableData = ko.mapping.fromJS(allData.data.documents);
				var array = observableData();
				self.myDocuments(array);
				self.projectID(allData.data.projectID);
				if(withHTML){
					// load document list html from Server
					cleanMainNode();
					$("#main").load("html/documentList",function() {
							applyBindingsMainNode(mainVM);
					});
				}
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.releaseDocument = function(document){
		var url = "document/publish/"+document._id.$oid();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				self.loadMyDocumentsPage(headVM.currentPage(),false);
			}else{
				showMessage("There are some Validation Errors - Check Editor", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			console.log(response);
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}else if(response.responseJSON.error.error=="genCMS validationFailed"){
				console.log("validation failed!");
				showMessage("There are some Validation Errors - Check Editor", "danger");
			}
		});
	}
	
	/**
	 * Take the Status back from released to inEdit
	 */
	self.unreleaseDocument = function(document){
		var url = "document/unpublish/"+document._id.$oid();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				self.loadMyDocumentsPage(headVM.currentPage(),false);
			}else{
				showMessage("The Document could not be taken back to Edit Mode", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			console.log(response);
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.deleteDocument = function(document){
		console.log("delete");
		var url = "document/delete/"+document._id.$oid();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				self.loadMyDocumentsPage(headVM.currentPage(),false);
			}else{
				showMessage("The Document could not be deleted", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			console.log(response);
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.goToEditDocument = function(document){
		location.hash = "editDocument/"+document._id.$oid();
	}
	
	/**
	 * Sorts the doc type list
	 */
	self.sortDocumentPage = function(sortBy){
		if(self.sortBy() == sortBy){	// Change Order
			self.sortAsc(!self.sortAsc());
		}else{							// Change Column
			self.sortBy(sortBy);
		}
		self.loadMyDocumentsPage(headVM.currentPage(),false);
	}
}

var CreateDocumentVM = function(availableDocTypes){
	var self = this;
	self.type="CreateDocumentVM";
	self.availableDocTypes = ko.observableArray(availableDocTypes);
	self.connectToDocument = ko.observable("");
	
	
	self.goToEditNewDocument = function(data) {
//		console.log(data);
//		console.log(data.docTypeId);
		// clean main node
		$('#main').addClass("hidden");
		$('#ajaxLoader').removeClass("hidden");
		ko.cleanNode(document.getElementById("main"));
		// create new Viewmodel
		mainVM = new EditDocumentVM();
		// create document on server
		var url = "/new/document/"+data._id.$oid;
		if(self.connectToDocument() != ""){
			url += "/"+self.connectToDocument();
		}
		$.getJSON(url, function(allData) {
			// mainVM.docType(ko.mapping.fromJS(allData));
			if (allData.res == "OK") {
				console.log(allData);
				mainVM.currentDocumentID(allData.data.document._id.$oid);
				mainVM.document(ko.mapping.fromJS(allData.data.document));
				mainVM.elements(ko.mapping.fromJS(allData.data.elements));
				mainVM.template(allData.data.template);
				mainVM.listTemplate(allData.data.listTemplate);
				
				// Set the location hash for going to the edit screen of the
				// created document
				location.hash = "editDocument/"+allData.data.document._id.$oid;
			} else {
				console.log("Creation of document failed");
				console.log(allData);
				showMessage(allData.error, "danger");
			}
		});
	}
}

var EditDocumentVM = function(){
	var self = this;
	self.type="EditDocumentVM";
	self.currentDocumentID = ko.observable("");
	self.elements = ko.observable({});
	self.document = ko.observable({});
	self.lookedUpAddresses = ko.observableArray([]);
	self.template = ko.observable("");
	self.listTemplate = ko.observable("");
	self.availableAudios = ko.observableArray([]);
	self.rejectMsg = ko.observable("");
	self.getLocale = function(field, locale){
		var name = field;
		
		var elms = ko.mapping.toJS(mainVM.elements());
		$.each(elms, function(){
			if(this.fname == field){
				$.each(this.locales, function(){
					if(this.loc == locale){
						name = this.val;
						return false;
					}
				});
				return false;
			}
		});
		return name;
	}
	
	self.addYoutubeVideo = function(url, fieldname){
		console.log("addYoutubeVideo");
		console.log(url);
		console.log(fieldname);
		console.log("id: "+youtubeUrlToId(url));
		var videoId = youtubeUrlToId(url);
		if(videoId == ""){
			// raise error
			console.log("no ID found");
			showMessage("No Youtube Video-ID found","danger");
		}else{
			console.log(self.document().fields[fieldname]());
			self.document().fields[fieldname]("YOUTUBE:"+videoId);
		}
	}
	
	self.previewDocument = function(){
		//var source = self.template();
		//var template = Handlebars.compile(source);
		var document = ko.mapping.toJS(self.document);
    	var fields = document.fields;
//    	console.log("fields");
    	if(typeof fields != "undefined"){
    		if(typeof fields.geoloc != "undefined"){
    			var lon = fields.geoloc.lon;
    			var lat = fields.geoloc.lat;
    			var display_name = fields.geoloc.display_name;
    			fields.geoloc = lon + genCMSseperator + lat + genCMSseperator + display_name + genCMSseperator + 0.2;
    		}
//    		console.log(fields);
    		//headVM.projectConnectionTemplates[document.connection].list(fields))
    		$('#documentEdit').addClass('hidden');	// hide edit
    		$('#documentPreview').removeClass('hidden');	// show preview
    		$('#documentPreviewList').addClass('hidden');
    		$('#documentPreviewFull').removeClass('hidden');
    		$('#documentPreviewFull').html(headVM.projectConnectionTemplates[document.connection].full(fields));
    		
    		initAudioPlayersOnPage();
    		initVideoPlayersOnPage();
    		initImagesOnPage();
    		setTimeout(function(){
    			initMapsOnPage();
    		},300);
    	}else{
    		console.log("FIELDS IS NOT DEFINED!");
    	}
	}
	

	self.previewDocumentList = function(){
		var document = ko.mapping.toJS(self.document);
    	var fields = document.fields;
//    	console.log("fields");
    	if(typeof fields != "undefined"){
    		if(typeof fields.geoloc != "undefined"){
    			var lon = fields.geoloc.lon;
    			var lat = fields.geoloc.lat;
    			var display_name = fields.geoloc.display_name;
    			fields.geoloc = lon + genCMSseperator + lat + genCMSseperator + display_name + genCMSseperator + 0.2;
    		}
    		console.log(fields);
    		$('#documentEdit').addClass('hidden');	// hide edit
    		$('#documentPreview').removeClass('hidden');	// show preview
    		$('#documentPreviewFull').addClass('hidden');
    		$('#documentPreviewList').removeClass('hidden');
    		$('#documentPreviewList').html(headVM.projectConnectionTemplates[document.connection].list(fields));
    		
    		initAudioPlayersOnPage();
    		initVideoPlayersOnPage();
    		initImagesOnPage();
    		setTimeout(function(){
    			initMapsOnPage();
    		},300);
    	}else{
    		console.log("FIELDS IS NOT DEFINED!");
    	}
	}
	
	self.closePreview = function(){
		$('#documentPreviewFull').html("");
		$('#documentPreviewList').html("");
		$('#documentPreview').addClass('hidden');
		$('#documentPreviewFull').addClass('hidden');
		$('#documentPreviewList').addClass('hidden');
		$('#documentEdit').removeClass('hidden');	// show edit
	}
	
	/**
	 * Save the changes of a doctype on the server or create a new one
	 */
	self.saveDocument = function(){
//		console.log("Save Document");
	
		// (ko.mapping.toJSON(self.selectedDocType(), null, 2));
		var saveUrl = "/document/" + self.currentDocumentID();

		$.ajax({
			url : saveUrl, // "/doctype/" +
			type : "POST",
			dataType : "json/xml/html/script", // expected format
			contentType : "application/json", // send as JSON
			data : ko.mapping.toJSON(self.document()),
			complete : function(returnedData) {
				if (returnedData.status == 200) {
					// alert("save successfull");
					var response = JSON.parse(returnedData.responseText);
					console.log(response);
					if(typeof response.data.validationErrors != 'undefined'){
						self.displayBackendValidationMessages(response.data.validationErrors);
						showMessage("Document successfully saved with some Validation Errors", "warning"); 	// possible
																											// types:
																											// success,
																											// info,
																											// warning,
																											// danger("Document
																											// Type
																											// successfully
																											// saved");
					}else{
						showMessage("Document successfully saved", "success"); 	// possible
																				// types:
																				// success,
																				// info,
																				// warning,
																				// danger("Document
																				// Type
																				// successfully
																				// saved");
					}
				}else{
					showMessage("Document Type could not be saved", "danger");
				}
			}
		});
	}
	
	self.releaseDocument = function(){
		var url = "document/publish/"+self.currentDocumentID();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				showMessage("Document published", "success");
				location.reload();
			}else{
				showMessage("There are some Validation Errors - Check Editor", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			console.log(response);
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}else if(response.responseJSON.error.error=="genCMS validationFailed"){
				console.log("validation failed!");
				showMessage("There are some Validation Errors - Check Editor", "danger");
			}
		});
	}
	
	self.confirmRelease = function(){
		var url = "document/publish/confirm/"+self.currentDocumentID();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				showMessage("Document Publish confirmed", "success");
				location.reload();
			}else{
				showMessage("Document Publish Confirmation failed", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			console.log(response);
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.rejectRelease = function(){
		var rejectUrl = "document/publish/reject/"+self.currentDocumentID();
		// reject msg -- json {msg:rejectmsg}
		var message = new Object()
		message.msg=self.rejectMsg();
//		console.log("Message is");
//		console.log(JSON.stringify(message));
//		console.log(message);
		$.ajax({
			url : rejectUrl,
			type : "POST",
			dataType : "json/xml/html/script", // expected format
			contentType : "application/json", // send as JSON
			data : JSON.stringify(message),
			complete : function(returnedData) {
				if (returnedData.status == 200) {
					showMessage("Document Publish successfully rejected", "success"); 	// possible
																						// types:
																						// success,
																						// info,
																						// warning,
																						// danger("Document
																						// Type
																						// successfully
																						// saved");
					location.reload();
				}else{
					showMessage("Document Publish could not be rejected", "danger");
				}
			}
		});
		
	}
	
	/**
	 * Take the Status back from released to inEdit
	 */
	self.unreleaseDocument = function(){
		var url = "document/unpublish/"+self.currentDocumentID();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				showMessage("Document is back in Edit Mode", "success");
				location.reload();
			}else{
				showMessage("The Document could not be taken back to Edit Mode", "danger");
			}
		}).fail(function( response, textStatus, error ) {
//			console.log( "Request Failed: " + textStatus + ", " + error );
//			console.log(response);
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.deleteDocument = function(){
//		console.log("delete");
		var url = "document/delete/"+self.currentDocumentID();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){
				showMessage("Document successfully deleted");
				headVM.goToMyDocumentList();
			}else{
				showMessage("The Document could not be deleted", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			console.log(response);
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	// displays the validation messages returned from Server
	self.displayBackendValidationMessages = function (validationErrors){
		// remove old messages
		$('[type=validation]').html('').addClass('hidden');
		
		$.each(validationErrors,function(){
//			console.log("FIELD:"+this.fieldname);
//			console.log("MSG:"+this.error);
			$('#Validation_'+this.fieldname).html(this.error).removeClass('hidden');
		});
		
	}
		
	self.goHistoryBack = function(){
//		console.log("Go BACK in History");
		history.back();
	}
	
	self.getAddress = function(fname){
//		console.log("get Address");
//		console.log(fname);
		var adr = self.document().fields[fname].address;
		if(adr==null){
			return "";
		}else{
			var a = ko.mapping.toJS(adr);
			console.log(a);
			// "69, Alte Poststra\u00dfe, Lend, Graz, Bezirk Graz, Steiermark,
			// 8020, \u00d6sterreich",
			// return adr.road+" "+adr.house_number+", "+adr.postal_code+"
			// "+adr.city+", "+adr.county+", "+adr.state;
			return JSON.stringify(a);
		}
	}
	
	self.selectAddress = function(index){
//		console.log("SELECTED ADDRESS: "+index);
		var address = ko.mapping.toJS(self.lookedUpAddresses()[index]);
		console.log(address);
		// self.document.fields.geoloc(ko.observable(ko.mapping.fromJS(geoLoc)));
		self.document().fields.geoloc.lat(address.lat);
		self.document().fields.geoloc.lon(address.lon);
		self.document().fields.geoloc.display_name(address.display_name);
		
		var doc = ko.mapping.toJS(mainVM.document)
		doc.fields.geoloc.address=address.address;
		self.document(ko.mapping.fromJS(doc));

		// clear temp markers and lookedUp Addresses
		self.lookedUpAddresses({});
		tempMarkers.clearLayers();
		// add new selected address to map layer
		selectedMarkers.clearLayers();
		selectedMarkers.addLayer(new L.Marker(L.latLng(address.lat,address.lon),{icon: selectedMarker}).bindPopup(mainVM.getDisplayName()));
		map.fitBounds(selectedMarkers.getBounds());
	}
	
	self.getDisplayName = function(){
		console.log("get displayname");
		return self.document().fields.geoloc.display_name();
	}
	
	self.lookupAddress = function(address){
		$('#addressInputHelp').addClass('hidden');
		$('#coordinatesInputHelp').addClass('hidden');
		$('#geolocAddrIcon').addClass("fa-spin");
		console.log("looking up: "+address);
		var url = "/geocodeAddress/"+encodeURIComponent(address);
		$.getJSON(url, function(allData) {
				if (allData.res == "OK") {
					self.lookedUpAddresses(ko.mapping.fromJS(allData.data)());
					// add markers to map if search result is not empty:
					// clear markers of old search if any
					tempMarkers.clearLayers();
					// add new markers if any search results
					if(self.lookedUpAddresses().length > 0){
//						console.log("add markers");
						$(self.lookedUpAddresses()).each(function(index, address){
							console.log
							tempMarkers.addLayer(new L.Marker(L.latLng(address.lat(),address.lon()),{icon: tempMarker}).bindPopup(address.display_name()+"<br><button type='button' class='btn btn-primary' onclick='mainVM.selectAddress("+index+"); clickBubble: false'><i class='fa fa-check'/></button>"));
						});
						// Fit Map to bounds of new markers
						map.fitBounds(tempMarkers.getBounds());
					}else{
						$('#addressInputHelp').removeClass('hidden');
					}
				}else{
					console.log(allData);
					
				}
				$('#geolocAddrIcon').removeClass("fa-spin");
		});
	}
	
	self.takeLatLon = function(latLon){
		$('#addressInputHelp').addClass('hidden');
		$('#coordinatesInputHelp').addClass('hidden');
		$('#geolocLatLonIcon').addClass("fa-spin");
		// var latLon = "47.12383875762813, 15.706152677448697";
		var split = latLon.split(",");
		var error=false;
		if(split.length==2){
		    console.log("may be lat, lon");
		    var lat = parseFloat(split[0]);
		    var lon = parseFloat(split[1]);
		    if(!isNaN(lat) && !isNaN(lon)){
		        console.log("lat lon found");
		        console.log("looking up latlon: "+latLon);
				var url = "/geocodeAddress/"+encodeURIComponent(latLon);
				$.getJSON(url, function(allData) {
						
						if (allData.res == "OK") {
							
							self.document().fields.geoloc.lat(lat);
							self.document().fields.geoloc.lon(lon);
							var doc = ko.mapping.toJS(mainVM.document)
//							console.log(allData.data);
//							console.log(allData.data.length);
							if(allData.data.length>0){
								console.log(allData.data[0]);
								doc.fields.geoloc.display_name=allData.data[0].display_name;
								doc.fields.geoloc.address=allData.data[0].address;
							}else{ // no address found
								doc.fields.geoloc.display_name="";
								doc.fields.geoloc.address={};
							}
							self.document(ko.mapping.fromJS(doc));
							
							self.lookedUpAddresses({});
							tempMarkers.clearLayers();
							// add new selected address to map layer
							selectedMarkers.clearLayers();
							selectedMarkers.addLayer(new L.Marker(L.latLng(lat,lon),{icon: selectedMarker}).bindPopup(mainVM.document().fields.geoloc.display_name()));
							map.fitBounds(selectedMarkers.getBounds());
						}else{
							error=true;
							console.log(allData);
						}
						$('#geolocLatLonIcon').removeClass("fa-spin");
				});
		    }else{
		    	error=true;
		    }
		}else{
		    error=true;
		}
		
		if(error){
			$('#geolocLatLonIcon').removeClass("fa-spin");
			$('#coordinatesInputHelp').removeClass('hidden');
		}
	}
	
	self.addTag = function(tag){
		if (self.checkTagUnique(tag) ){
			var url = "/document/tag/add/"+self.currentDocumentID()+"/"+encodeURIComponent(tag);
			$.getJSON(url, function(allData) {
				if(allData.res="OK"){
					self.document().tags.push(tag);
				}else{
					showMessage("Document Tag could not be added", "danger");
				}
			}).fail(function( response, textStatus, error ) {
				console.log( "Request Failed: " + textStatus + ", " + error );
				if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
					cleanMainNode();
					loadForbiddenPageMainNode();
				}
			});
		}else{
			console.log("Tag already exists");
		}
	}
	
	self.removeTag = function(tag){
		var url = "/document/tag/remove/"+self.currentDocumentID()+"/"+encodeURIComponent(tag);
		$.getJSON(url, function(allData) {
			if(allData.res="OK"){
				self.document().tags.remove(tag);
			}else{
				showMessage("Document Tag could not be removed", "danger");
			}
		}).fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.checkTagUnique = function(tag){
//		console.log("check tag "+ tag);
//		console.log(self.document().tags());
		if(self.document().tags.indexOf(tag) < 0) {
			return true;
		}else{
			return false;
		}
	}
	
	self.init = function(){
//		console.log("init document editor!!!");
		setTimeout(function(){
		
			// initPolyfill for Number Input
			$(':input[type="number"]').inputNumber();

			/*
			 * if field geoloc exists
			 */
			// init the map
			var mapExists = initMap();
			if(mapExists){
				// add click handler for address lookup
				map.on('click', function(e) {
					console.log("Lat, Lon : " + e.latlng.lat + ", " + e.latlng.lng);
					$('#addressInput').val(e.latlng.lat + ", " + e.latlng.lng);
				});
				// add resize event to map
				$(window).on("resize", function() {
					$("#map").height($(window).height()*0.45);
					map.invalidateSize();
				}).trigger("resize");			
				// check if location is given - if so - add marker to map!
				// clean lookedUpAddresses
				self.lookedUpAddresses({});
				// clean tempMarkers
				tempMarkers.clearLayers();
				// clean selectedMarkers
				selectedMarkers.clearLayers();
				if(self.document().fields.geoloc.lat()!=null && self.document().fields.geoloc.lon() != null){
					selectedMarkers.addLayer(new L.Marker(L.latLng(self.document().fields.geoloc.lat(),self.document().fields.geoloc.lon()),{icon: selectedMarker}).bindPopup(self.document().fields.geoloc.display_name()));
				}
				/*
				 * if field geoloc lat & lon is set -> add marker to map!
				 */
			}
			// TODO 18022014
			
			
		    },500);
		
		return "";
	}
	
}

var DocTypeVM = function(){
	var self = this;
	self.type="DocTypeVM";
	self.docTypes = ko.observableArray([]);
	self.selectedDocType = ko.observable({});
	self.localeOptions = ko.observableArray([ {"val":"de", "text":"German"}, 
	                                          {"val":"en", "text":"English"},
	                                          {"val":"es", "text":"Spanish"},
	                                          {"val":"fr", "text":"French"} ]);
	self.selectedLocaleOption = ko.observable("de");
	self.docTypeConnection = ko.observable(ko.mapping.fromJSON('{"name":"","description":"","active":false}'));
	self.docTypesPerPage = 10;
	self.projectID = ko.observable(0);
	self.currentPage = ko.observable(0);
	self.availableStyles = ko.observableArray([]);
	self.availablePages = ko.observable(0);
	self.sortAsc = ko.observable(true);
	self.sortBy = ko.observable("_id");
	self.searchProjectOnly = ko.observable(false);
	self.searchUserOnly = ko.observable(false);
	self.searchProjectOnly.subscribe(function(updated){
		self.loadDocTypePage(0);
	});
	self.searchUserOnly = ko.observable(false);
	self.searchUserOnly.subscribe(function(updated) {
		self.loadDocTypePage(0);
	});
	self.newFieldName=ko.observable("");
	self.newFieldNameUnique=ko.observable(false);
	self.newFieldName.subscribe(function(updated){
		self.newFieldNameUnique( self.checkNewFieldNameUniqueAndOK(self.newFieldName()) );
	});
	
	self.checkNewFieldNameUniqueAndOK = function(newName){
		if (newName === "geoloc") { // reserverd key word
			return false;
		}
		if (isChar(newName)) {
			var match = ko.utils.arrayFirst(self.selectedDocType().elems(),
					function(item) {
						return newName === item.fname();
					});
			if (match) {
				return false;
			} else {
				return true;
			}
		}else{
			return false;
		}
	}
	
	self.goToDocTypesList = function() {
		deleteMessage();
		location.hash = "docTypes";
	};
	
	self.goToEditDocType = function(docType) {
		deleteMessage();
		location.hash = "editDocType" + "/" + docType._id.$oid();
	};

	self.goToEditNewDocType = function() {
		deleteMessage();
		location.hash = "editDocType/0";
	}

	self.goToEditDocTypeDesign = function(docType) {	// edit DocType Detail
														// Design
		location.hash = "editDocTypeDesign"+ "/" + docType._id.$oid();
	};
	
	self.goToEditDocTypeListDesign = function(docType) {	// edit DocType List
															// Design
		location.hash = "editDocTypeListDesign"+ "/" + docType._id.$oid();
	};
	
	self.goToConnectDocTypeToProject = function(docType){
//		console.log("goToConnectDocTypeProject");
		location.hash = "docTypeConnect/" + docType._id.$oid();
	}
	
	/**
	 * loads the docTypes from the server (with pagination) and loads the view
	 * if not already loaded
	 */
	self.loadDocTypePage = function(page){
		// load Data from Server
		self.currentPage(page);
		var url = "/doctypes/"+self.docTypesPerPage+"/"+self.currentPage()+"/"+self.searchProjectOnly()+"/"+self.searchUserOnly()+"/"+self.sortBy()+"/"+self.sortAsc();
		$.getJSON(url, function(allData) {
			if(allData.res=="OK"){
				self.availablePages(Math.ceil(allData.data.results/self.docTypesPerPage));
				var observableData = ko.mapping.fromJS(allData.data.doctypes);
				var array = observableData();
				self.docTypes(array);
				self.projectID(allData.data.projectID);
				if($("#docTypeList").length == 0){ // load docType Editor from
													// Server
					// ko.cleanNode(document.getElementById("main"));
					cleanMainNode();
					$("#main").load("html/docTypeEditor",function() {
						applyBindingsMainNode(mainVM);
						// ko.applyBindings(mainVM,
						// document.getElementById("main"));
						$("#docTypeList").show();
						$("#docTypeEdit").hide();
					});
				}else{
					$("#docTypeList").show();
					$("#docTypeEdit").hide();
				}
			}
		}) .fail(function( response, textStatus, error ) {
			console.log( "Request Failed: " + textStatus + ", " + error );
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	/**
	 * Sorts the doc type list
	 */
	self.sortDocTypePage = function(sortBy){
		if(self.sortBy() == sortBy){	// Change Order
			self.sortAsc(!self.sortAsc());
		}else{							// Change Column
			self.sortBy(sortBy);
		}
		self.loadDocTypePage(self.currentPage());
	}
	
	/**
	 * adds a simple TextField to the document Type
	 */
	self.addTextLineField = function() {
		// alert(self.selectedDocType().elems());
		var name = self.newFieldName();
		self.newFieldName("");
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"textLine","locales":[{"loc":"en", "val":""}],"min":-1, "max":-1, "required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
	}
	
	/**
	 * adds a html field to the document type
	 */
	self.addHtmlField = function() {
		var name = self.newFieldName();
		self.newFieldName("");
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"HTML","locales":[{"loc":"en", "val":""}],"required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
	}
	
	/**
	 * adds an image field to the document type
	 */ 
	self.addImageField = function() {
		var name = self.newFieldName();
		self.newFieldName("");
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"img","locales":[{"loc":"en", "val":""}],"minWidth":-1,"maxWidth":-1,"minHeight":-1,"maxHeight":-1,"required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
	}
	
	/**
	 * adds an audio field to the document type
	 */
	self.addAudioField = function() {
		var name = self.newFieldName();
		self.newFieldName("");
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"audio","locales":[{"loc":"en", "val":""}],"maxSize":-1,"required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
	}
	
	/**
	 * adds a video field to the document type
	 */
	self.addVideoField = function() {
		var name = self.newFieldName();
		self.newFieldName("");
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"video","locales":[{"loc":"en", "val":""}],"maxSize":-1,"required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
	}
	
	/**
	 * adds a number field to the document type
	 */
	self.addNumberField = function() {
		var name = self.newFieldName();
		self.newFieldName("");
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"number","locales":[{"loc":"en", "val":""}],"min":-1,"max":-1,"required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
	}
	
	/**
	 * adds a boolean field to the document type
	 */
	self.addBooleanField = function() {
		var name = self.newFieldName();
		self.newFieldName("");
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"boolean","locales":[{"loc":"en", "val":""}],"required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
	}
	
	/**
	 * adds a location field to the document type & sets isPOI of document to
	 * true
	 */
	self.addLocationField = function() {
		var name = "geoloc";
		self.selectedDocType().elems
				.push(ko.mapping
						.fromJSON('{"fname":"'
								+ name
								+ '","type":"geoLoc","locales":[{"loc":"en", "val":""}],"required":true,"sortOrder":'
								+ (self.selectedDocType().elems().length + 1)
								+ '}'));
		self.selectedDocType().isPOI(true);
	}

	/**
	 * adds a locale to the document type field
	 */
	self.addLocale = function(docField) {
		docField.locales.push(ko.mapping.fromJSON('{"loc":"'
				+ self.selectedLocaleOption() + '", "val":""}'));
	}

	/**
	 * deletes a locale from the document type field
	 */
	self.deleteLocale = function(locale, docTypeField) {
		docTypeField.locales.remove(locale);
	}

	/**
	 * deletes a field from the document type
	 */
	self.deleteField = function(docTypeField) {
//		console.log(docTypeField);
//		console.log(docTypeField.type());
		if(docTypeField.type()==="geoLoc"){
			self.selectedDocType().isPOI(false);
		}
		self.selectedDocType().elems.remove(docTypeField);
	}

	/**
	 * move a field up in the document type (sort order is changed)
	 */
	self.moveFieldUp = function(docTypeField) {
		var index = ko.utils.arrayIndexOf(self.selectedDocType().elems(), docTypeField);
		if (index > 0) {
			var tmp = self.selectedDocType().elems()[index].sortOrder();
			self.selectedDocType().elems()[index].sortOrder(self.selectedDocType().elems()[(index - 1)].sortOrder());
			self.selectedDocType().elems()[(index - 1)].sortOrder(tmp);
		}
		self.selectedDocType().elems.sort(function(left, right) {
			return left.sortOrder() == right.sortOrder() ? 0 : (left.sortOrder() < right.sortOrder() ? -1 : 1)
		});
	}

	/**
	 * move a field down in the document type (sort order is changed)
	 */
	self.moveFieldDown = function(docTypeField) {
		var index = ko.utils.arrayIndexOf(self.selectedDocType().elems(),docTypeField);
		if (index < self.selectedDocType().elems().length - 1) {
			var tmp = self.selectedDocType().elems()[index].sortOrder();
			self.selectedDocType().elems()[index].sortOrder(self.selectedDocType().elems()[(index + 1)].sortOrder());
			self.selectedDocType().elems()[(index + 1)].sortOrder(tmp);
		}
		self.selectedDocType().elems.sort(function(left, right) {
			return left.sortOrder() == right.sortOrder() ? 0 : (left.sortOrder() < right.sortOrder() ? -1 : 1);
		});
	}
	
	/**
	 * Save the changes of a doctype on the server or create a new one
	 */
	self.saveDocType = function(docType) {
		// (ko.mapping.toJSON(self.selectedDocType(), null, 2));
		if (typeof self.selectedDocType()._id === 'undefined') {
			var saveUrl = "/new/doctype";
		} else {
			var saveUrl = "/doctype/" + self.selectedDocType()._id.$oid();
		}
		$.ajax({
			url : saveUrl, // "/doctype/" +
			type : "POST",
			dataType : "xml/html/script/json", // expected format
			contentType : "application/json", // send as JSON
			data : ko.mapping.toJSON(self.selectedDocType()),
			complete : function(returnedData) {
				if (returnedData.status == 200) {
					// alert("save successfull");
					if (saveUrl == "/new/doctype") { // new document
						// - load new Data from Server!
						location.hash = "editDocType" + "/" + JSON.parse(returnedData.responseText).data._id;
					}
					showMessage("Document Type successfully saved", "success"); 	// possible
																					// types:
																					// success,
																					// info,
																					// warning,
																					// danger("Document
																					// Type
																					// successfully
																					// saved");
				}else{
					showMessage("Document Type could not be saved", "danger");
				}
			}
		});
	}

	/**
	 * Delete the currently selected DocType TODO: prompt if it should really be
	 * deleted
	 */
	self.deleteDocType = function(docType) {
		if(typeof self.selectedDocType()._id == 'undefined'){	// docType not
																// saved on
																// server - just
																// go back to
																// list of
																// doctypes
			showMessage("Document Type successfully deleted", "success");
			self.currentPage(0);
			location.hash = "docTypes";
		}else{
			$.get("del/doctype/" + self.selectedDocType()._id.$oid(),function(data) {
//				console.log("deleted");
//				console.log(data);
				if(data.res == "OK"){
					showMessage("Document Type \""+self.selectedDocType().name()+"\" successfully deleted", "success");
					self.currentPage(0);
					location.hash = "docTypes";		
				}else{
					showMessage("Document Type could not be deleted", "danger");
				}
			}).fail(function( response, textStatus, error ) {
				console.log( "Request Failed: " + textStatus + ", " + error );
				if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
					cleanMainNode();
					loadForbiddenPageMainNode();
				}
			});
		}
	}

	/**
	 * create a copy of the doctype
	 */
	self.copyDocType = function(docType) {
		// copy is created server side
		$.get("copy/doctype/" + docType._id.$oid(), function(data) {
//			console.log(data);
			if(data.res == "OK"){
				showMessage("Document Type successfully copied", "success");
				location.hash = "editDocType" + "/" + data.data._id;
			}else{
				showMessage("Document Type could not be copied", "danger");
				console.log("ERROR");
			}
		});
	}
	
	/**
	 * Create a new Connection between DocumentType and selected Project
	 */
	self.connectDocTypeToProject = function(){
		var newConnection = JSON.stringify({"name" : "teST", "description" : '<p><strong>Dies ist eine Beschreibung!<br /></strong></p>\n<p><em><strong>Und die ist Toll!<script> alert "seppö";</strong></em></p>', active : false });
		$.ajax({
			url : "/doctype/connect/"+self.selectedDocType()._id.$oid(),
			type : "POST",
			dataType : "xml/html/script/json", // expected format
			contentType : "application/json", // send as JSON
			data : ko.mapping.toJSON(self.docTypeConnection),
			complete : function(returnedData) {
				var response = JSON.parse(returnedData.responseText);
				if(response.res == "OK"){
						// reload doctype
						$.getJSON("/doctype/"+self.selectedDocType()._id.$oid(),function(allData) {
							self.docTypeConnection(ko.mapping.fromJSON('{"name":"","description":"","active":false}'));
							self.selectedDocType(ko.mapping.fromJS(allData));
						});
						showMessage("Connection to Project was successfully created", "success");
				}else{
					
					showMessage(response.error.error, "danger");
				}
			}
		});
	}
	
	/**
	 * Update an exsiting Connection between DocType and the selected Project
	 */
	self.updateDocTypeToProjectConnection = function(connection){
		$.ajax({
			url : "/doctype/connect/"+self.selectedDocType()._id.$oid(),
			type : "POST",
			dataType : "xml/html/script/json", // expected format
			contentType : "application/json", // send as JSON
			data : ko.mapping.toJSON(connection),
			complete : function(returnedData) {
				var response = JSON.parse(returnedData.responseText);
				if(response.res == "OK"){
					showMessage("Connection was successfully updated", "success");
				}else{
					showMessage(response.error.error, "danger");
				}
			}
		});
	}

	/**
	 * Delete the connection between the DocType and the selected Project
	 */
	self.disconnectDocTypeFromProject = function(connection){
//		console.log("disconnect Doctype from project");
//		console.log(ko.toJSON(connection));
		var deleteUrl = "/doctype/connect/"+self.selectedDocType()._id.$oid()+"/"+connection._id.$oid();  
		$.ajax({
			url : deleteUrl, // "/doctype/" +
			type : "DELETE",
			complete : function(returnedData) {
//				console.log(returnedData);
				if (returnedData.status == 200) {
					// reload doctype
					$.getJSON("/doctype/"+self.selectedDocType()._id.$oid(),function(allData) {
						self.docTypeConnection(ko.mapping.fromJSON('{"name":"","description":"","active":false}'));
						self.selectedDocType(ko.mapping.fromJS(allData));
					});
					showMessage("Connection was successfully deleted", "success");
				}else{
					showMessage("Connection could not be deleted", "danger");
				}
			}
		});
	}
	
	// ############### Design Editor #############
	self.initDesignEditor = function () {
	    $("body").css("min-height", $(window).height() - 90);
	    $(".demo").css("min-height", $(window).height() - 160);
	    $(".demo, .demo .column").sortable({
	        connectWith: ".column",
	        opacity: .35,
	        handle: ".drag"
	    });
	    $(".sidebar-nav .lyrow").draggable({
	        connectToSortable: ".demo",
	        helper: "clone",
	        handle: ".drag",
	        drag: function (e, t) {
	            t.helper.width(400)
	        },
	        stop: function (e, t) {
	            $(".demo .column").sortable({
	                opacity: .35,
	                connectWith: ".column"
	            })
	        }
	    });
	    $(".sidebar-nav .box").draggable({
	        connectToSortable: ".column",
	        helper: "clone",
	        handle: ".drag",
	        drag: function (e, t) {
	            t.helper.width(400)
	        },
	        stop: function () {
	        }
	    });
	    $("#editDesign").click(function () {
	        $("#download-Layout").hide();
	    	$("#docTypeDesignEditor").removeClass("devpreview sourcepreview");
	        $("#docTypeDesignEditor").addClass("edit");
	        self.designRemoveMenuClasses();
	        $("#edit-Layout").show();
	        $(this).addClass("active");
	        return false
	    });
	    $("#previewDesign").click(function () {
	    	$("#edit-Layout").hide();
	        $("#docTypeDesignEditor").removeClass("edit");
	        $("#docTypeDesignEditor").addClass("devpreview sourcepreview");
	        self.designRemoveMenuClasses();
	        self.generateDesignLayout();
	        $("#download-Layout").show();
	        $(this).addClass("active");
	        return false
	    });
	    $(".nav-header").click(function () {
	        $(".sidebar-nav .boxes, .sidebar-nav .rows").hide();
	        $(this).next().slideDown()
	    });
	    self.designRemoveElm();
	    // Add Function to element style dropdown
		$(".demo").delegate(" a.elementStyleDropdown", "click", function (e) {
			e.preventDefault();
			var element = $(this).parent().parent().next().next().children();
			var ul = $(this).next().children();
			ul.each( // check if classes are assigned
				function(){
					var item = $(this).children(":first");
					if(element.hasClass($(item).prop("value"))){ // set
																	// checked
						$(item).prop("checked", true);
					}else{ // clear checked
						$(item).prop("checked", false);
					}
			});
		});
		// Add Function to element style dropdown checkboxes
	    $(".demo").delegate(".configuration .elementStyleDropdown input", "click", function (e) {
	        e.preventDefault();
	        var t = $(this).parent().parent();
	        var element = t.parent().parent().next().next().children();
			if($(this).prop("checked")){
				$(element).addClass($(this).prop("value"));
			}else{
				$(element).removeClass($(this).prop("value"));
			}
	    });
	    // Add Function to column style dropdown
		$(".demo").delegate(" a.columnStyleDropdown", "click", function (e) {
			e.preventDefault();
			var column = $(this).parent().parent().parent();
			var ul = $(this).next().children();
			ul.each( // check if classes are assigned
				function(){
					var item = $(this).children(":first");
					if(column.hasClass($(item).prop("value"))){ // set checked
						$(item).prop("checked", true);
					}else{ // clear checked
						$(item).prop("checked", false);
					}
			});
		});
		// Add Function to element style dropdown checkboxes
	    $(".demo").delegate(".configuration .columnStyleDropdown input", "click", function (e) {
	        e.preventDefault();
	        var t = $(this).parent().parent();
	        var column = t.parent().parent().parent();
			if($(this).prop("checked")){
				$(column).addClass($(this).prop("value"));
			}else{
				$(column).removeClass($(this).prop("value"));
			}
	    });
	};
	self.rebuildDesignHTML = function(designJSON){
		var html = "";
		var currentRow = null;
		var currentColumns = null;
		var counter = 0;
		var cloneTarget = '#edit-Container';
		function rebuildHTMLrec(data, cloneTo){
			var returnVal = "";
			if(data.typ == 'container'){
				// container, handle only childs
				data.childs.forEach(
						function(child){
							returnVal += rebuildHTMLrec(child, cloneTo);
						});
			}else if(data.typ == 'row'){
				// find out column design of childs first...
				var columns = new Array(); 
				data.childs.forEach(
						function(child){
							columns.push(child.classes.match(/\d+/g));
						});
				colVal = columns.join(" ");
				// set value of columns...
				$('#restoreLayoutInput').val(colVal);
				$('#restoreLayoutInput').keyup();
				// clone to container
				currentRow = $('#restoreLayout').clone(true,true).removeAttr('id');
				currentRow.appendTo(cloneTo);
				// call rebuild for child columns
				currentColumns = $(currentRow).children().next().next().next().children().children();
				data.childs.forEach(
						function(child){
							rebuildHTMLrec(child, cloneTo);
							currentColumns = $(currentColumns).next();
						});
			}else if(data.typ == 'column'){
				var currentColumn = currentColumns.first(); 
				// currentColumns.first().html(counter);
				// cloneTarget = currentColumns.first();
				counter++;
				var cloneConfig = $('#restore-ColumnConfig').children().first().clone(true,true);
				$(currentColumn).append(cloneConfig);
				// TODO add TMP to classes!!
				$(currentColumn).prop('class',data.classes.replace(/visible/g,"TMPvisible").replace(/hidden/g,"TMPhidden"));
				// call rebuild for childs
				data.childs.forEach(
						function(child){
							rebuildHTMLrec(child, currentColumn);
						});
			}else if(data.typ == 'element'){
				// get first element from menue
				var cloneElement = $('#elements').children().first().clone(true,true);
				cloneElement.children().next().next().next().next().children().text(self.helperGetFieldLocale(data.field)());
				cloneElement.children().next().next().next().next().children().prop("class",data.classes);
				cloneElement.children().next().next().next().next().children().attr("fieldname",data.field);
				cloneElement.children().next().next().next().next().children().attr("type","element");
				cloneElement.children().next().next().next().next().children().attr("ftype",data.ftype);
				// TODO set attr fieldname & type element
				$(cloneTo).append(cloneElement);
			}else if(data.typ == 'label'){
				var cloneElement = $('#elements').children().first().clone(true,true);
				cloneElement.children().next().next().next().next().children().text(self.helperGetFieldLocale(data.field)()+": ");
				cloneElement.children().next().next().next().next().children().prop("class",data.classes);
				cloneElement.children().next().next().next().next().children().attr("fieldname",data.field);
				cloneElement.children().next().next().next().next().children().attr("type","label");
				// TODO set attr fieldname & type element
				$(cloneTo).append(cloneElement);
			}
		}
		rebuildHTMLrec(designJSON, cloneTarget);
		return html;
	}
	
	self.designRemoveMenuClasses = function() {
	    $("#menu-layoutit li button").removeClass("active")
	}
	
	self.designRemoveElm = function() {
	    $(".demo").delegate(".remove", "click", function (e) {
	        e.preventDefault();
	        $(this).parent().remove();
	        if (!$(".demo .lyrow").length > 0) {
	            self.designClear();
	        }
	    })
	}
	
	self.designClear = function(){
		    $(".demo").empty()
	}
	
	self.cleanHtml = function(e) {
		$(e).parent().append($(e).children().html());
	}
	
	self.changeStructure = function(e, t) {
	    $("#download-Layout ." + e).removeClass(e).addClass(t)
	}
	
	self.saveDesignLayout = function() {
		self.saveDesign("/doctypeDesign/");
	}
	
	self.saveListDesignLayout = function(){
		self.saveDesign("/doctypeListDesign/");
	}
	
	self.saveDesign = function(url){
		self.generateDesignLayout();
		var jsonData = self.handleDesignChildren($('#download-Layout > :first-child'));
//		console.log(jsonData);
		var saveUrl = url + self.selectedDocType()._id.$oid();
		$.ajax({
			url : saveUrl,
			type : "POST",
			dataType : "xml/html/script/json", // expected format
			contentType : "application/json", // send as JSON
			data : jsonData,
			complete : function(returnedData) {
				if (returnedData.status == 200) {
					showMessage("Document Type Design successfully saved", "success");
				}else{
					showMessage("Document Type Design could not be saved", "danger");
				}
			}
		});
	}
	
	self.handleDesignChildren = function(e) {
		var element = new Object();
		element.classes = $(e).prop('class');
		if($(e).hasClass('container') || $(e).hasClass('row') || $(e).hasClass('column')){
			var childs = new Array();
			$(e).children().each(
				function(){
					var childVal = self.handleDesignChildren(this);
					if(childVal!=''){
						childs.push(childVal);
					}
				});
			element.childs = childs;
			element.field = '';
			element.ftype = '';
			if($(e).hasClass('container')){
				element.typ = 'container';
				return JSON.stringify(element);
			}else if($(e).hasClass('row')){
				element.typ = 'row';
			}else if($(e).hasClass('column')){
				element.typ = 'column';
			}
			return element;
		}else if($(e).prop('tagName')=='SPAN'){
			// Element or Label
			element.field = $(e).attr('fieldname');
			element.typ = $(e).attr('type');	// element or label
			element.ftype = $(e).attr('ftype');	// std, image, audio,...
			element.childs = new Array();
			return element;
		} else {
			return '';
		}
	}
	
	self.generateDesignLayout = function() {
	    var e = "";
	    $("#download-Layout").children().html($(".demo").html());
	    var t = $("#download-Layout").children();
	    t.find(".preview, .configuration, .drag, .remove").remove();
	    t.find(".lyrow").addClass("removeClean");
	    t.find(".box-element").addClass("removeClean");
	    t.find(".lyrow .lyrow .lyrow .lyrow .lyrow .removeClean").each(function () {
	        self.cleanHtml(this)
	    });
	    t.find(".lyrow .lyrow .lyrow .lyrow .removeClean").each(function () {
	    	self.cleanHtml(this)
	    });
	    t.find(".lyrow .lyrow .lyrow .removeClean").each(function () {
	    	self.cleanHtml(this)
	    });
	    t.find(".lyrow .lyrow .removeClean").each(function () {
	    	self.cleanHtml(this)
	    });
	    t.find(".lyrow .removeClean").each(function () {
	    	self.cleanHtml(this)
	    });
	    t.find(".removeClean").each(function () {
	    	self.cleanHtml(this)
	    });
	    t.find(".removeClean").remove();
	    
	    // remove Temp Bootstrap Classes and replace them with correct ones
	    t.find(".TMPvisible-xs").addClass("visible-xs");
	    t.find(".TMPvisible-xs").removeClass("TMPvisible-xs");
	    t.find(".TMPvisible-sm").addClass("visible-sm");
	    t.find(".TMPvisible-sm").removeClass("TMPvisible-sm");
	    t.find(".TMPvisible-md").addClass("visible-md");
	    t.find(".TMPvisible-md").removeClass("TMPvisible-md");
	    t.find(".TMPvisible-lg").addClass("visible-lg");
	    t.find(".TMPvisible-lg").removeClass("TMPvisible-lg");
	    t.find(".TMPhidden-xs").addClass("hidden-xs");
	    t.find(".TMPhidden-xs").removeClass("TMPhidden-xs");
	    t.find(".TMPhidden-sm").addClass("hidden-sm");
	    t.find(".TMPhidden-sm").removeClass("TMPhidden-sm");
	    t.find(".TMPhidden-md").addClass("hidden-md");
	    t.find(".TMPhidden-md").removeClass("TMPhidden-md");
	    t.find(".TMPhidden-lg").addClass("hidden-lg");
	    t.find(".TMPhidden-lg").removeClass("TMPhidden-lg");
	    
	    $("#download-Layout .column").removeClass("ui-sortable");
	    $("#download-Layout .row-fluid").removeClass("clearfix").children().removeClass("column");
	    if ($("#download-Layout .container").length > 0) {
	    	self.changeStructure("row-fluid", "row")
	    }
	    formatSrc = $.htmlClean($("#download-Layout").html(), {
	        format: true,
	        allowedAttributes: [
	            ["id"],
	            ["class"],
	            ["data-toggle"],
	            ["data-target"],
	            ["data-parent"],
	            ["role"],
	            ["data-dismiss"],
	            ["aria-labelledby"],
	            ["aria-hidden"],
	            ["data-slide-to"],
	            ["data-slide"],
	            ["type"],
	            ["fieldname"],
	            ["ftype"]
	        ]
	    });
	    $("#download-Layout").html(formatSrc);
	}
	
	// ####################### Helper
	self.helperGetFieldLocale = function(fieldName){
		var locale = $.cookie("locale");
		var locales = "";
		var name = "";
		if(locale == null){
			locale = "en";
		}
		// get correct field-locales
		$.each(self.selectedDocType().elems(),function(){
			if(this.fname()==fieldName){
				locales = ko.toJS(this.locales);
				return false; // break
			}
		});
		// get locale
		$.each(locales,function(){
			if(this.loc==locale){
				name = this.val;
				return false;
			}
			if(this.loc == "en"){ // default
				name = this.value;
			}
		});
		if(name == ""){ // no locale found, no english locale found --> fallback
			return ko.computed(function () {
			       return fieldName;
		    });
		}else{
			return ko.computed(function () {
				return name
			}); 
		}
	}
}

var UserViewVM = function(){
	var self = this;
	self.type="UserViewVM";
	self.documentsPerPage=10;
	self.documentsList = ko.observableArray([]);
	self.selectedDocument = ko.observable(null);
	self.availableTags = ko.observable(null);
	self.currentPage = ko.observable(0);
	self.availablePages = ko.observable(0);
	self.filtered = ko.observable(false);
	self.editorial = ko.observable(null);
	self.time = ko.observable();
	self.selectedPOIS = ko.observable(null);
	self.initSearchMap = ko.observable(false);
	self.sortAsc = ko.observable(true);
	self.sortBy = ko.observable("modifiedAt.$date");
	self.connectedDocs = ko.observable(null);
	self.connectedDocsPerPage=3;
	self.connectedDocsCurrentPage=ko.observable(0);
	self.connectedDocsAvailablePages = ko.observable(0);
		
	self.loadIndexPage = function(page, withEditorial, withHTML){
		self.currentPage(page);
		var url = "";
		var loadEditorial = false;
    
		if(page==0 && headVM.selectedProjectEditorial()!="" && headVM.filterTags().length == 0 && headVM.filterType()==="" && headVM.selectedTag() ===""){
			//page 0 and unfiltered -> show editorial
			self.loadEditorial(true, withHTML);
		}else{
			self.editorial(null);
			self.loadNewestDocuments(withHTML);
		}
	}
	
	self.loadEditorial = function(loadNewestDocument, withHTML){
		var url = "/doc/"+headVM.selectedProjectEditorial();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){	// Editorial loaded
//				console.log("Editorial loaded!");
				self.editorial(ko.mapping.fromJS(allData.data));
				if(loadNewestDocument){
					self.loadNewestDocuments(withHTML);
				}
			}else{
				console.log("Some Error occured");
				console.log(allData);
			}
		}).fail(function( response, textStatus, error ) {
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.loadNewestDocuments = function(withHTML, toMap){
		url = "/docs/"+self.documentsPerPage+"/"+self.currentPage();
		var tagFilter = ko.mapping.toJS(headVM.filterTags);
		if(headVM.selectedTag() != ""){
			tagFilter.push(headVM.selectedTag());
		}
		var postData = {
				"orderBy": "fields.modifiedAt",
				"asc" : false,
				"tags": tagFilter,
				"connectionID" : headVM.filterType()
				}
		$.ajax({
			url : url,
			type : "POST",
			dataType : "json", // expected format
			contentType : "application/json", // send as JSON
			data : JSON.stringify(postData),
			complete : function(returnedData) {
//				console.log(returnedData);
				if (returnedData.status == 200 && returnedData.responseJSON.res==="OK") { // List of newest Documents loaded!
					// List of newest Documents loaded
					self.availablePages(Math.ceil(returnedData.responseJSON.data.results/self.documentsPerPage));
					
					self.documentsList(ko.mapping.fromJS(returnedData.responseJSON.data.documents)());
					if(withHTML){
//						console.log("load html");
						//load Available Types from Server
						headVM.loadAvailableTypes();
						//load Available Tags from Server
						headVM.loadAvailableTags();
						// load document list html from Server
						cleanMainNode();
						$("#main").load("html/index",function() {
							$('#mainContent').removeClass('hidden');
							$('#fullDoc').addClass('hidden');
							$('#searchMap').addClass('hidden');
							applyBindingsMainNode(mainVM);
							$(document).scrollTop(0);
							if(toMap){
								self.openMapSearch();
							}
						});
					}else{
//						console.log("load NO html");
						$('#mainContent').removeClass('hidden');
						$('#fullDoc').addClass('hidden');
						$('#searchMap').addClass('hidden');
						$(document).scrollTop(0);
					}
				}else{
					console.log("Some Error occured");
					console.log(allData);
					showMessage("Page could not be loaded", "danger");
				}
			}
		});
	}
	
	
	self.loadNewestConnectedDocuments = function(page){
//		self.connectedDocs = ko.observable(null);
//		self.connectedDocsPerPage=5;
//		self.connectedDocsCurrentPage=ko.observable(0);
//		self.connectedDocsAvailablePages = ko.observable(0);
		self.connectedDocsCurrentPage(page);
		url = "/docs/"+self.connectedDocsPerPage+"/"+self.connectedDocsCurrentPage();
		var tagFilter = ko.mapping.toJS(headVM.filterTags);
		
		var postData = {
				"orderBy": "fields.modifiedAt",
				"asc" : false,
				"tags": tagFilter,
				"connectionID" : headVM.filterType(),
				"connectedToDoc" : self.selectedDocument().id()
				}
		$.ajax({
			url : url,
			type : "POST",
			dataType : "json", // expected format
			contentType : "application/json", // send as JSON
			data : JSON.stringify(postData),
			complete : function(returnedData) {
				if (returnedData.status == 200 && returnedData.responseJSON.res==="OK") { // List of newest Documents loaded!
					console.log("got connected docs");
					self.connectedDocsAvailablePages(Math.ceil(returnedData.responseJSON.data.results/self.connectedDocsPerPage));
					self.connectedDocs(ko.mapping.fromJS(returnedData.responseJSON.data.documents)());
					$('#connectedDocuments').removeClass('hidden');
				}else{
					console.log("Some Error occured");
					console.log(allData);
					showMessage("Connected Documents could not be loaded", "danger");
				}
			}
		});
	}
	
	/**
	 * Click handler for documents in list view
	 */
	self.openDoc = function(data, event) {
//		console.log("you clicked " + event.target);
//		console.log(event.target);
		if($(event.target).is('[class*="jp-"]')){
//			console.log("You fool clicked the jplayer!");
		}else if($(event.target).is('[class*="leaflet-"]')){
//			console.log("You clicked the map!");
		}else{
//			console.log("got it!");
//			console.log(data._id.$oid());
			if(location.hash=="#doc/"+data._id.$oid()){
				self.loadDocument(data._id.$oid(),false);
			}else{
				location.hash="doc/"+data._id.$oid();
			}
		}
	}
	
	self.goToEditDocument = function(){
		location.hash = "editDocument/"+self.selectedDocument().id();
	}

	//TODO: add HTML T/F
	self.loadDocument = function(docID,withHTML){
		var url = "/doc/"+docID;
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){	// Editorial loaded
//				console.log("Document loaded!");
				self.selectedDocument(ko.mapping.fromJS(allData.data));
				self.loadNewestConnectedDocuments(0);
				if(withHTML){
					// load document list html from Server
					cleanMainNode();
					$("#main").load("html/index",function() {
						$('#connectedDocs').collapse('hide');
						$('#connectedDocuments').addClass('hidden');
						$('#mainContent').addClass('hidden');
						$('#fullDoc').removeClass('hidden');
						$('#searchMap').addClass('hidden');
						applyBindingsMainNode(mainVM);
						$(document).scrollTop(0);
					});
				}else{
					$('#connectedDocs').collapse('hide');
					$('#connectedDocuments').addClass('hidden');
					$('#mainContent').addClass('hidden');
					$('#fullDoc').removeClass('hidden');
					$('#searchMap').addClass('hidden');
					$(document).scrollTop(0);
				}
			}else{
				console.log("Some Error occured");
				console.log(allData);
			}
		}).fail(function( response, textStatus, error ) {
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	self.goToCreateConnectedDocument = function(){
		console.log("gotocreateconnectedDocument");
		deleteMessage();

		location.hash = "createDocument/"+self.selectedDocument().id();
	}
	
	self.setSelectedDocEditorial = function(){
		var url = "/project/editorial/"+self.selectedDocument().id();
		$.getJSON(url, function(allData) {
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){	// Editorial loaded
//				console.log("Document set as Editorial!");
				headVM.selectedProjectEditorial(self.selectedDocument().id());
			}else{
				console.log("Some Error occured");
				console.log(allData);
			}
		}).fail(function( response, textStatus, error ) {
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
	
	/**
	 * Sorts the doc type list
	 */
	self.sortDocumentPage = function(sortBy){
		if(self.sortBy() == sortBy){	// Change Order
			self.sortAsc(!self.sortAsc());
		}else{							// Change Column
			self.sortBy(sortBy);
		}
		self.loadMyDocumentsPage(headVM.currentPage(),false);
	}
	
	self.goToDocument = function(docID){
		console.log();
		location.hash = "doc/"+docID;
	}
	
	self.openMapSearch = function(){
		$('#mainContent').addClass('hidden');
		$('#fullDoc').addClass('hidden');
		$('#searchMap').removeClass('hidden');
    	var id =  "map";
    	//TODO Default from Project lat, lon
    	setTimeout(function(){
	    	var mapExists = initMap(id, 47.0786521, 15.4070155, 13);
	    	if(mapExists){
				// add click handler for address lookup
				/*map.on('click', function(e) {
					console.log("Lat, Lon : " + e.latlng.lat + ", " + e.latlng.lng);
					$('#addressInput').val(e.latlng.lat + ", " + e.latlng.lng);
				})*/;
				// add resize event to map
				$(window).on("resize", function() {
					$("#map").height($(window).height()*0.5);
					map.invalidateSize();
				}).trigger("resize");			
				// check if location is given - if so - add marker to map!
				// clean lookedUpAddresses
				askForLocationsOnMap();
				map.on('moveend', onMapMove);
	    	}
    	},200);
	}
	
	self.setAuthorRoleJobDescription = function(role){
		$('#addJobDocIcon').addClass("fa-spin");
		var url = "/project/authorrole/setJobDesc/"+role+"/"+self.selectedDocument().id();
		$.getJSON(url, function(allData) {
			$('#addJobDocIcon').removeClass("fa-spin");
//			console.log("response from "+url);
//			console.log(allData);
			if(allData.res=="OK"){	// Editorial loaded
//				console.log("Document set as Job Description!");
				showMessage("Document set as Job Description!", "success");
				//headVM.selectedProjectEditorial(self.selectedDocument().id());
			}else{
				console.log("Some Error occured");
				console.log(allData);
			}
		}).fail(function( response, textStatus, error ) {
			if(response.responseJSON.error=="Not authorized" || response.responseJSON.error=="Credentials required"){
				cleanMainNode();
				loadForbiddenPageMainNode();
			}
		});
	}
}

var headVM = new HeaderVM();
// headVM.loadProjectTitles();
// headVM.loadLoggedInUser();
var mainVM = new Object();
// var bodyVM = new
	// var viewModel = new genCMSViewModel();
// ko.applyBindings(new genCMSRootViewModel());
ko.applyBindings(headVM, document.getElementById("header"));