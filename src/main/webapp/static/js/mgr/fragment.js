var editor;
var upeditor;
$(document).ready(
	function() {
		editor = editormd("editor", {
			width : "100%",
			height : 400,
			 watch            : false,
             toolbar          : false,
             codeFold         : true,
             searchReplace    : true,
             theme            : "default",
             mode             : "text/html",
             path : basePath + '/static/editor/markdown/lib/'
		});
		upeditor = editormd("upeditor", {
			width : "100%",
			height : 400,
			 watch            : false,
             toolbar          : false,
             codeFold         : true,
             searchReplace    : true,
             theme            : "default",
             mode             : "text/html",
             path : basePath + '/static/editor/markdown/lib/'
		});
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
			editor.resize();
		}).on('hidden.bs.modal', function() {
			editor.clear();
		});
		$("#updateUserFragmentModal").on("show.bs.modal", function() {
			clearTip();
			$("#updateUserFragmentModal").find("form")[0].reset();
		}).on("shown.bs.modal",function(){
			upeditor.resize();
		}).on('hidden.bs.modal', function() {
			upeditor.setValue("");
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
			if(data == ""){
				bootbox.alert("要更新的挂件不存在");
			} else {
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
