jQuery.prototype.serializeObject=function(){  
    var obj= {};  
    $.each(this.serializeArray(),function(index,param){  
          obj[param.name]=param.value;  
    });  
    return obj;  
}; 

var token = $("meta[name='_csrf']").attr("content");
var header = $("meta[name='_csrf_header']").attr("content");
if (token != null && header != null && token != "" && header != "") {
	$(document).ajaxSend(function(e, xhr, options) {
		xhr.setRequestHeader(header, token);
	});
}

function success(msg) {
	$(".tip")
			.html('<div class="alert alert-success">' + msg + '</div>');
}

function error(msg) {
	$(".tip").html('<div class="alert alert-danger">' + msg + '</div>');
}

function clearTip() {
	$(".tip").html('');
}