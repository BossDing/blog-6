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
		$("#createUserFragementModal").on("show.bs.modal", function() {
			clearTip();
			$("#createUserFragementModal").find("form")[0].reset();
		}).on("shown.bs.modal",function(){
			editor.resize();
		}).on('hidden.bs.modal', function() {
			editor.clear();
		});
		$("#updateUserFragementModal").on("show.bs.modal", function() {
			clearTip();
			$("#updateUserFragementModal").find("form")[0].reset();
		}).on("shown.bs.modal",function(){
			upeditor.resize();
		}).on('hidden.bs.modal', function() {
			upeditor.setValue("");
		});
		$("#show-create").click(function(){
			$('#createUserFragementModal').modal('show')
		})
	$("[data-action='remove']").click(function(){
		var me = $(this);
		bootbox.confirm("确定要删除吗?",function(result){
			if(!result){
				return ;
			}
			$.ajax({
				type : "post",
				url : basePath+"/mgr/fragement/user/delete",
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
		$("#updateUserFragementModal").find("form").find("input[type=checkbox]").change(function(){
			if($(this).prop("checked")){
				$("#updateUserFragementModal").find("form").find("select[name=space]").parent().hide();
			}else{
				$("#updateUserFragementModal").find("form").find("select[name=space]").parent().show();
			}
		})
		$("#createUserFragementModal").find("form").find("input[type=checkbox]").change(function(){
			if($(this).prop("checked")){
				$("#createUserFragementModal").find("form").find("select[name=space]").parent().hide();
			}else{
				$("#createUserFragementModal").find("form").find("select[name=space]").parent().show();
			}
		})
	$("[data-action='edit']").click(function(){
		$.get(basePath+"/mgr/fragement/user/get/"+$(this).attr("data-id"),{},function(data){
			if(data == ""){
				bootbox.alert("要更新的挂件不存在");
			} else {
				$("#updateUserFragementModal").modal("show");
				var form = $("#updateUserFragementModal").find('form');
				form.find("input[name='name']").val(data.name);
				form.find("input[name='id']").val(data.id);
				form.find("input[type=checkbox]").prop("checked",data.global);
				if(data.space){
					form.find("select[name='space']").val(data.space.id);
				}
				if(data.global)
					$("#updateUserFragementModal").find("form").find("select[name=space]").parent().hide();					
				form.find("textarea[name='description']").val(data.description);
				upeditor.setValue(data.tpl);
			}
		});
	});
	
	$("#updateUserFragement").click(
			function() {
				$("#updateUserFragement").prop("disabled", true);
				var data = $("#updateUserFragementModal").find("form").serializeObject();
				delete data['upeditor-markdown-doc'];
				var space = data.space;
				delete data['space'];
				data.global = $("#updateUserFragementModal").find("form").find("input[type=checkbox]").prop("checked");
				if(space != ''){
					data.space = {"id":space};
				}
				data.tpl = upeditor.getValue();
				$.ajax({
					type : "post",
					url : basePath + "/mgr/fragement/user/update",
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
						$("#updateUserFragement").prop("disabled",
								false);
					}
				});
			});

	$("#createUserFragement").click(
			function() {
				$("#createUserFragement").prop("disabled", true);
				var data = $("#createUserFragementModal").find("form").serializeObject();
				delete data['editor-markdown-doc'];
				var space = data.space;
				delete data['space'];
				if(space != ''){
					data.space = {"id":space};
				}
				data.global = $("#createUserFragementModal").find("form").find("input[type=checkbox]").prop("checked");
				data.tpl = editor.getValue();
				$.ajax({
					type : "post",
					url : basePath + "/mgr/fragement/user/create",
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
						$("#createUserFragement").prop("disabled",
								false);
					}
				});
			});
	});
