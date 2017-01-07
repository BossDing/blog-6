var editor;
var upeditor;
$(document).ready(
	function() {
		var mixedMode = {
		        name: "htmlmixed",
		        scriptTypes: [{matches: /\/x-handlebars-template|\/x-mustache/i,
		                       mode: null},
		                      {matches: /(text|application)\/(x-)?vb(a|script)/i,
		                       mode: "vbscript"}]
		      };
	    
	    upeditor = CodeMirror.fromTextArea(document.getElementById("upeditor"), {
	        mode: mixedMode,
	        lineNumbers: true,
	        autoCloseTags: true,
	        extraKeys: {"Ctrl-Space": "autocomplete"}
	      });
	    upeditor.setSize('100%',400);
		$("[data-page]").click(function(){
			var page = $(this).attr("data-page");
			$("#pageForm").find("input[name='currentPage']").val(page);
			$("#pageForm").submit();
		})
		$('[data-toggle="tooltip"]').tooltip();
		$("#createUserFragmentModal").on("show.bs.modal", function() {
			clearTip();
			$("#createUserFragmentModal").find("form")[0].reset();
		}).on("shown.bs.modal",function(){
			if(!editor){
				editor = CodeMirror.fromTextArea(document.getElementById("editor"), {
			        mode: mixedMode,
			        lineNumbers: true,
			        autoCloseTags: true,
			        extraKeys: {"Ctrl-Space": "autocomplete"}
			      });
			    editor.setSize('100%',400);
			}
		}).on('hidden.bs.modal', function() {
		});
		$("#updateUserFragmentModal").on("show.bs.modal", function() {
			clearTip();
			$("#updateUserFragmentModal").find("form")[0].reset();
		}).on("shown.bs.modal",function(){
		}).on('hidden.bs.modal', function() {
		});
		$("#show-create").click(function(){
			$('#createUserFragmentModal').modal('show')
		})
	$("[data-action='remove']").click(function(){
		var me = $(this);
		bootbox.confirm("确定要删除吗?",function(result){
			if(!result){
				return ;
			}
			$.ajax({
				type : "post",
				url : basePath+"/mgr/fragment/user/delete",
				data : {"id":me.attr("data-id")},
				success : function(data){
					if(data.success){
						success(data.message);
						setTimeout(function(){
							window.location.reload();
						},500)
					} else {
						error(data.message);
					}
				},
				complete:function(){
				}
			});
		});
	});
		$("#updateUserFragmentModal").find("form").find("input[type=checkbox]").change(function(){
			if($(this).prop("checked")){
				$("#updateUserFragmentModal").find("form").find("select[name=space]").parent().hide();
			}else{
				$("#updateUserFragmentModal").find("form").find("select[name=space]").parent().show();
			}
		})
		$("#createUserFragmentModal").find("form").find("input[type=checkbox]").change(function(){
			if($(this).prop("checked")){
				$("#createUserFragmentModal").find("form").find("select[name=space]").parent().hide();
			}else{
				$("#createUserFragmentModal").find("form").find("select[name=space]").parent().show();
			}
		})
	$("[data-action='edit']").click(function(){
		$.get(basePath+"/mgr/fragment/user/get/"+$(this).attr("data-id"),{},function(data){
			if(!data.success){
				bootbox.alert("要更新的挂件不存在");
			} else {
				data = data.data;
				$("#updateUserFragmentModal").modal("show");
				var form = $("#updateUserFragmentModal").find('form');
				form.find("input[name='name']").val(data.name);
				form.find("input[name='id']").val(data.id);
				form.find("input[type=checkbox]").prop("checked",data.global);
				if(data.space){
					form.find("select[name='space']").val(data.space.id);
				}
				if(data.global)
					$("#updateUserFragmentModal").find("form").find("select[name=space]").parent().hide();					
				form.find("textarea[name='description']").val(data.description);
				upeditor.setValue(data.tpl);
			}
		});
	});
	
	$("#updateUserFragment").click(
			function() {
				$("#updateUserFragment").prop("disabled", true);
				var data = $("#updateUserFragmentModal").find("form").serializeObject();
				delete data['upeditor-markdown-doc'];
				var space = data.space;
				delete data['space'];
				data.global = $("#updateUserFragmentModal").find("form").find("input[type=checkbox]").prop("checked");
				if(space != ''){
					data.space = {"id":space};
				}
				data.tpl = upeditor.getValue();
				$.ajax({
					type : "post",
					url : basePath + "/mgr/fragment/user/update",
					data : JSON.stringify(data),
					dataType : "json",
					contentType : 'application/json',
					success : function(data) {
						if (data.success) {
							success(data.message);
							setTimeout(function() {
								window.location.reload();
							}, 500)
						} else {
							error(data.message);
						}
					},
					complete : function() {
						$("#updateUserFragment").prop("disabled",
								false);
					}
				});
			});

	$("#createUserFragment").click(
			function() {
				$("#createUserFragment").prop("disabled", true);
				var data = $("#createUserFragmentModal").find("form").serializeObject();
				delete data['editor-markdown-doc'];
				var space = data.space;
				delete data['space'];
				if(space != ''){
					data.space = {"id":space};
				}
				data.global = $("#createUserFragmentModal").find("form").find("input[type=checkbox]").prop("checked");
				data.tpl = editor.getValue();
				$.ajax({
					type : "post",
					url : basePath + "/mgr/fragment/user/create",
					data : JSON.stringify(data),
					dataType : "json",
					contentType : 'application/json',
					success : function(data) {
						if (data.success) {
							success(data.message);
							setTimeout(function() {
								window.location.reload();
							}, 500)
						} else {
							error(data.message);
						}
					},
					complete : function() {
						$("#createUserFragment").prop("disabled",
								false);
					}
				});
			});
	});
