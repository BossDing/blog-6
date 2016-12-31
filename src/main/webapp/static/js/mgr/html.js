var editor;
var publishing = false;
var tags = [];
var init = false;
$(document).ready(function(){
	CKEDITOR.plugins.addExternal( 'codemirror', basePath+'/static/editor/ckeditor/plugins/codemirror/' );
	CKEDITOR.plugins.addExternal( 'prettifycode', basePath+'/static/editor/ckeditor/plugins/prettifycode/','plugin.js' );
	editor = CKEDITOR.replace('editor', {
		extraPlugins : 'codemirror,prettifycode'
	});
	CKEDITOR.config.enterMode = CKEDITOR.ENTER_BR;
	CKEDITOR.config.shiftEnterMode = CKEDITOR.ENTER_P;
	CKEDITOR.config.allowedContent = true;
	CKEDITOR.config.height=800;
	editor.addCommand( 'openFileDialog', {
        exec: function( editor ) {
        	fileSelectPageQuery(1,'');
        	$("#fileSelectModal").modal("show");
        }
    });
	
	editor.ui.addButton('file', {
         label   : '文件选择',
         command : 'openFileDialog',
         icon : basePath+'/static/editor/ckeditor/plugins/file/file.png'
     });
	editor.on( 'loaded', function( evt ) {
		init = true;
		getStyles();
	} );
	
	$('.form_datetime').datetimepicker({
        language:  'zh-CN',
        weekStart: 1,
		autoclose: 1,
		todayHighlight: 1,
		startView: 2,
		forceParse: 0,
        showMeridian: 1
    });

	$("#status").change(function() {
		if ($(this).val() == 'SCHEDULED') {
			$("#scheduleContainer").show();
		} else {
			$("#scheduleContainer").hide();
		}
	});
	
	var oldTags = $("#oldTags").val();
	if(oldTags != ''){
		var oldTagArray = oldTags.split(",");
		for(var i=0;i<oldTagArray.length;i++){
			var tag = oldTagArray[i];
			if(tag != ''){
				addTag(tag);
				renderTag();
			}
		}
	}
	
	$("#space").change(function(){
		var val = $(this).val();
		getStyles();
	});
	var oldSpace = $("#oldSpace").val();
	if(oldSpace != ""){
		$("#space").val(oldSpace);
	}

	$("#tags-input").keypress(function(e) {
		var me = $(this);
		// 回车键事件  
		if (e.which == 13) {
			addTag($.trim(me.val()));
			renderTag();
			me.val("");
		}
	});
	
	setInterval(function(){
		save();
	}, 10*1000)
	
	$("#submit-art").click(function(){
		publishing = true;
		var me = $(this);
		var article = {};
		article.title = $("#title").val();
		article.content = editor.getData();
		article.from = $("#from").val();
		article.status = $("#status").val();
		if(article.status == 'SCHEDULED'){
			article.pubDate = $("#scheduleDate").val()
		};
		if($("#level").val() != ''){
			article.level = $("#level").val();
		}
		article.isPrivate = $("#private").prop("checked");
		article.allowComment = $("#allowComment").prop("checked");
		article.hidden = $("#hidden").prop("checked");
		article.tags = tags;
		article.summary = $("#summary").val();
		article.space = {"id":$("#space").val()};
		article.editor = "HTML";
		if($("#lockId").val() != ""){
			article.lockId = $("#lockId").val();
		}
		article.alias = $("#alias").val();
		
		me.prop("disabled",true);
		var url = basePath+"/mgr/article/write";
		if($("#id").val() != ""){
			article.id = $("#id").val();
		}
		$.ajax({
			type : "post",
			url : url,
            contentType:"application/json",
			data : JSON.stringify(article),
			success : function(data){
				if(data.success){
					bootbox.alert("保存成功");
					setTimeout(function(){
						window.location.href = basePath+'/mgr/article/index';
					},500)
				} else {
					bootbox.alert(data.message);
					publishing = false;
				}
			},
			complete:function(){
				me.prop("disabled",false);
			}
		});
	})
	$.get(basePath + '/mgr/lock/all',{},function(data){
		var oldLock = $("#oldLock").val();
		if(data.success){
			var locks = data.data;
			if(locks.length > 0){
				var html = '';
				html += '<div class="form-group">'
				html += '<label for="lockId" class="control-label">锁:</label> ';
				html += '<select id="lockId" class="form-control">';
				html += '<option value="">无</option>';
				for(var i=0;i<locks.length;i++){
					var lock = locks[i];
					if(lock.id == oldLock){
						html += '<option value="'+lock.id+'" selected="selected">'+lock.name+'</option>';
					}else{
						html += '<option value="'+lock.id+'">'+lock.name+'</option>';
					}
				}
				html += '</select>';
				html += '</div>';
				$("#lock_container").html(html);
			}
		}else{
			console.log(data.data);
		}
	});
})

function getStyles(){
	var id = $("#space").val();
	$.get(basePath+'/mgr/page/sys/getStyles',{"id":id},function(data){
		if(data.success){
			var csses = data.data.csses;
			if(csses.length > 0){
				var text = '';
				for(var i=0;i<csses.length;i++){
					var css = csses[i];
					text += css;
					text += '\r\n';
				}
				$("#csses").val(text);
			}
			if(data.data.style){
				$("#styles").val(data.data.style);
			}else{
				$("#styles").val("");
			}
			changeEditorCss(csses,data.data.style);
		}else{
			//ignore
		}
	})
}

function showStyleBox(){
	if(init){
		$("#styleModal").modal("show");
	}
}

function addStyles(){
	var csses = $("#csses").val().split("\n");
	var styles = $("#styles").val();
	var _csses = [];
	if(csses.length > 0){
		for(var i=0;i<csses.length;i++){
			var css = csses[i];
			if(isCss(css)){
				_csses.push(css);
			}
		}
	}
	changeEditorCss(_csses,styles);
	$("#styleModal").modal("hide");
}

function isCss(link){
	var ext = link.split('.').pop();
	if(ext.toLowerCase() == 'css'){
		return true;
	}else{
		var index = ext.indexOf('?');
		if(index != -1){
			ext = ext.substring(0,index);
			if(ext.toLowerCase() == 'css'){
				return true;
			}
		}
	}
	return false;
}

function changeEditorCss(csses,style){
	if(csses && csses.length > 0){
		CKEDITOR.config.contentsCss  = csses;
	}
	if(style && $.trim(style).length > 0){
		CKEDITOR.addCss(style);
	}
	if (editor.mode == 'wysiwyg') {
 		editor.setMode('source',function(){
 	 		editor.setMode('wysiwyg')
 		});
    }
}

		function showTagError(error) {
			if ($("#tag-tip").length == 0)
				$("#tags-input").before(error);
			setTimeout(function() {
				$("#tag-tip").remove();
			}, 1000);
		}

		function addTag(tag) {
			var tag = $.trim(tag);
			if (tags.length >= 10) {
				showTagError('<div id="tag-tip" class="alert alert-danger">最多只能有10标签</div>')
			} else if (tag == "" || tag.length > 20) {
				showTagError('<div id="tag-tip" class="alert alert-danger">标签名在1~20个字符之间</div>')
			} else {
				for (var i = 0; i < tags.length; i++) {
					var _tag = tags[i];
					if (_tag.name == tag) {
						me.val("");
						return;
					}
				}
				tags.push({
					"name" : $.trim(tag)
				});
			}
		}

		function renderTag() {
			if(tags.length == 0){
				$("#tags-container").html('');
				return ;
			}
			var html = '<div class="table-responsive"><table class="table table-borderless">';
			if (tags.length > 5) {
				html += '<tr>';
				for (var i = 0; i < 5; i++) {
					html += getLabel_html(tags[i].name);
				}
				html += '</tr>';
				html += '<tr>';
				for (var i = 5; i < tags.length; i++) {
					html += getLabel_html(tags[i].name);
				}
				html += '</tr>';
			} else {
				html += '<tr>';
				for (var i = 0; i < tags.length; i++) {
					html += getLabel_html(tags[i].name);
				}
				html += '</tr>';
			}
			html += '<table></div>';
			$("#tags-container").html(html);
		}
		
		function save(){
			if(publishing){
				return ;
			}
			var article = {};
			article.title = $("#title").val();
			if($.trim(article.title) == ""){
				article.title = new Date().format("yyyy-mm-dd HH:MM:ss");
			}
			article.content = editor.getData();
			if($.trim(article.content) == ''){
				return ;
			}
			article.from = $("#from").val();
			article.status = 'DRAFT';
			if($("#level").val() != ''){
				article.level = $("#level").val();
			}
			article.isPrivate = $("#private").prop("checked");
			article.allowComment = $("#allowComment").prop("checked");
			article.hidden = $("#hidden").prop("checked");
			article.tags = tags;
			article.summary = $("#summary").val();
			article.space = {"id":$("#space").val()};
			article.editor = "HTML";
			if($("#lockId").val() != ""){
				article.lockId = $("#lockId").val();
			}
			article.alias = $("#alias").val();
			
			var url = basePath+"/mgr/article/write";
			if($("#id").val() != ""){
				article.id = $("#id").val();
			}
			publishing = true;
			$.ajax({
				type : "post",
				url : url,
	            contentType:"application/json",
				data : JSON.stringify(article),
				success : function(data){
					if(data.success){
						$("#id").val(data.data.id);
					}
				},
				complete:function(){
					publishing = false;
				}
			});
		}

		function getLabel_html(tag) {
			return '<td><span class="label label-success">'
					+ tag
					+ '<a href="###" onclick="removeTag($(this))" style="margin-left:5px"><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a></span></td>';
		}

		function removeTag(o) {
			var tag = o.parent().text();
			for (var i = 0; i < tags.length; i++) {
				if (tags[i].name == tag) {
					tags.splice(i, 1);
					renderTag();
					return;
				}
			}
		}