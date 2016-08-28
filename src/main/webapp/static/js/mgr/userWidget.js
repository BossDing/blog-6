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
		$("#createUserWidgetModal").on("show.bs.modal", function() {
			clearTip();
			$("#createUserWidgetModal").find("form")[0].reset();
		}).on("shown.bs.modal",function(){
			editor.resize();
		}).on('hidden.bs.modal', function() {
			editor.clear();
		});
		$("#updateUserWidgetModal").on("show.bs.modal", function() {
			clearTip();
			$("#updateUserWidgetModal").find("form")[0].reset();
		}).on("shown.bs.modal",function(){
			upeditor.resize();
		}).on('hidden.bs.modal', function() {
			upeditor.setValue("");
		});
		$("#show-create").click(function(){
			$('#createUserWidgetModal').modal('show')
		})
	$("[data-action='remove']").click(function(){
		var me = $(this);
		bootbox.confirm("确定要删除吗?",function(result){
			if(!result){
				return ;
			}
			$.ajax({
				type : "post",
				url : basePath+"/mgr/widget/user/delete",
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
	
	$("[data-action='edit']").click(function(){
		$.get(basePath+"/mgr/widget/user/get/"+$(this).attr("data-id"),{},function(data){
			if(data == ""){
				bootbox.alert("要更新的挂件不存在");
			} else {
				$("#updateUserWidgetModal").modal("show");
				var form = $("#updateUserWidgetModal").find('form');
				form.find("input[name='name']").val(data.name);
				form.find("input[name='id']").val(data.id);
				form.find("textarea[name='description']").val(data.description);
				upeditor.setValue(data.defaultTpl);
			}
		});
	});
	
	$("#updateUserWidget").click(
			function() {
				$("#updateUserWidget").prop("disabled", true);
				var data = $("#updateUserWidgetModal").find(
						"form").serializeObject();
				data.defaultTpl = upeditor.getValue();
				$.ajax({
					type : "post",
					url : basePath + "/mgr/widget/user/update",
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
						$("#updateUserWidget").prop("disabled",
								false);
					}
				});
			});

	$("#createUserWidget").click(
			function() {
				$("#createUserWidget").prop("disabled", true);
				var data = $("#createUserWidgetModal").find(
						"form").serializeObject();
				data.defaultTpl = editor.getValue();
				$.ajax({
					type : "post",
					url : basePath + "/mgr/widget/user/create",
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
						$("#createUserWidget").prop("disabled",
								false);
					}
				});
			});
	});
