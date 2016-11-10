var editor;
var publishing = false;
		var tags = [];
		$(function() {
			$("#doCommentConfig").click(function(){
				var check = $(this).prop('checked');
				if(check)
					$("#commentConfigContainer").show();
				else
					$("#commentConfigContainer").hide();
			})
			editor = editormd("editormd", {
				width : "100%",
				height : 800,
				toolbarIcons : function() {
					return  [
					            "undo", "redo", "|", 
					            "bold", "del", "italic", "quote", "ucwords", "uppercase", "lowercase", "|", 
					            "h1", "h2", "h3", "h4", "h5", "h6", "|", 
					            "list-ul", "list-ol", "hr", "|",
					            "link", "reference-link", "image", "code", "preformatted-text", "code-block", "table", "datetime", "emoji", "html-entities", "pagebreak", "|",
					            "goto-line", "watch", "preview","clear", "search","fileIcon", "|",
					            "help", "info"
					        ]
		        },
		        toolbarIconsClass : {
		           fileIcon : "fa-file"  // 指定一个FontAawsome的图标类
		        },
		        toolbarHandlers : {
		            fileIcon : function(cm, icon, cursor, selection) {
		            	this.executePlugin("blogFile", "blog-file/file-dialog");  
		            }
		        },

		        lang : {
		            toolbar : {
		               fileIcon : "文件"  // 自定义按钮的提示文本，即title属性
		            }
		        },
				tex : true,
				tocm : true,
				emoji : true,
				taskList : true,
				codeFold : true,
				searchReplace : true,
				flowChart : true,
				sequenceDiagram : true,
				saveHTMLToTextarea : true,
				path : basePath + '/static/editor/markdown/lib/'
			});
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
    			article.content = editor.getMarkdown();
    			article.from = $("#from").val();
    			article.status = $("#status").val();
    			if(article.status == 'SCHEDULED'){
    				article.pubDate = $("#scheduleDate").val()
    			};
    			if($("#level").val() != ''){
    				article.level = $("#level").val();
    			}
    			article.isPrivate = $("#private").prop("checked");
    			article.hidden = $("#hidden").prop("checked");
    			article.tags = tags;
    			article.summary = $("#summary").val();
    			article.space = {"id":$("#space").val()};
    			article.editor = "MD";
    			if($("#lockId").val() != ""){
    				article.lockId = $("#lockId").val();
    			}
    			article.alias = $("#alias").val();
    			if($("#doCommentConfig").is(':checked')){
    				var commentConfig = {};
        			commentConfig.allowComment = $("#allowComment").prop("checked");
        			commentConfig.commentMode = $("#commentMode").val();
        			commentConfig.asc = $("#commentSort").val();
        			commentConfig.allowHtml = $("#allowHtml").prop("checked");
        			commentConfig.limitSec = $("#limitSec").val();
        			commentConfig.limitCount = $("#limitCount").val();
        			commentConfig.check = $("#check").prop("checked");
    				commentConfig.pageSize = $("#pageSize").val();
        			article.commentConfig = commentConfig;
    			}
    			me.prop("disabled",true);
    			var url = basePath+"/mgr/article/write?autoDraft=false";
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
		});

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
				article.title = editormd.dateFormat("yyyy-MM-dd HH:mm");
			}
			article.content = editor.getMarkdown();
			if($.trim(article.content) == ''){
				return ;
			}
			article.from = $("#from").val();
			if($("#oldStatus").val() != 'DRAFT'){
				article.status = $("#status").val();
				if(article.status == 'SCHEDULED'){
					article.pubDate = $("#scheduleDate").val();
					if(!article.pubDate || article.pubDate == ''){
						article.status = 'DRAFT';
					}
				};
			}else{
				article.status = 'DRAFT';
			}
			if($("#level").val() != ''){
				article.level = $("#level").val();
			}
			article.isPrivate = $("#private").prop("checked");
			article.hidden = $("#hidden").prop("checked");
			article.tags = tags;
			article.summary = $("#summary").val();
			article.space = {"id":$("#space").val()};
			article.editor = "MD";
			if($("#lockId").val() != ""){
				article.lockId = $("#lockId").val();
			}
			article.alias = $("#alias").val();
			
			if($("#doCommentConfig").is(':checked')){
				var commentConfig = {};
				commentConfig.allowComment = $("#allowComment").prop("checked");
				commentConfig.commentMode = $("#commentMode").val();
				commentConfig.asc = $("#commentSort").val();
				commentConfig.allowHtml = $("#allowHtml").prop("checked");
				commentConfig.limitSec = $("#limitSec").val();
				commentConfig.limitCount = $("#limitCount").val();
				commentConfig.pageSize = $("#pageSize").val();
				if(commentConfig.limitSec < 1 || commentConfig.limitSec > 300){
					commentConfig.limitSec = 60;
				}
				if(commentConfig.limitCount <1 || commentConfig.limitCount>100){
					commentConfig.limitCount = 10;
				}
				commentConfig.check = $("#check").prop("checked");
				article.commentConfig = commentConfig;
			}
			var url = basePath+"/mgr/article/write?autoDraft=true";
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
