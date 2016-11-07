$(document).ready(function() {
	$("#spaceModal").on("show.bs.modal",function(){
		clearTip();
		$(this).find("form")[0].reset();
	})
	$("#editSpaceModal").on("show.bs.modal",function(event){
		clearTip();
		$(this).find("form")[0].reset();
		var a = $(event.relatedTarget);
		var id = a.attr("data-id");
		var modal = $(this)
		$.get(basePath+'/mgr/space/get/'+id,{},function(data){
			if(data.success){
				data = data.data;
				modal.find('.modal-body input[name="name"]').val(data.name);
				modal.find('.modal-body input[name="alias"]').val(data.alias);
				modal.find('.modal-body input[name="isPrivate"]').prop("checked",data.isPrivate);
				modal.find('.modal-body input[name="isDefault"]').prop("checked",data.isDefault);
				modal.find('.modal-body input[name="articleHidden"]').prop("checked",data.articleHidden);
				modal.find('.modal-body input[name="id"]').val(id);
				if(data.lockId){
					modal.find('.modal-body select[name="lockId"]').val(data.lockId)
				} else {
					modal.find('.modal-body select[name="lockId"]').val('');
				}
			}else{
				bootbox.alert(data.message);
			}
		});
	});
	$.get(basePath + '/mgr/lock/all',{},function(data){
		if(data.success){
			var locks = data.data;
			if(locks.length > 0){
				var html = '';
				html += '<div class="form-group">'
				html += '<label for="lockId" class="control-label">锁:</label> ';
				html += '<select name="lockId" class="form-control">';
				html += '<option value="">无</option>';
				for(var i=0;i<locks.length;i++){
					var lock = locks[i];
					html += '<option value="'+lock.id+'">'+lock.name+'</option>';
				}
				html += '</select>';
				html += '</div>';
				$(".lock_container").html(html);
			}
		}else{
			console.log(data.data);
		}
	});
	
	$('[data-toggle="tooltip"]').tooltip();
	$("#create").click(function() {
		clearTip();
		$("#create").prop("disabled",true);
		var data = $("#spaceModal").find("form").serializeObject();
		data.isPrivate = $("#spaceModal").find('input[name="isPrivate"]').is(":checked");
		data.articleHidden = $("#spaceModal").find('input[name="articleHidden"]').is(":checked");
		data.isDefault = $("#spaceModal").find('input[name="isDefault"]').is(":checked");
		$.ajax({
			type : "post",
			url : basePath+"/mgr/space/add",
			data : JSON.stringify(data),
			dataType : "json",
			contentType : 'application/json',
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
				$("#create").prop("disabled",false);
			}
		});
	});
	
	$("#update").click(function() {
		clearTip();
		$("#update").prop("disabled",true);
		var data = $("#editSpaceModal").find("form").serializeObject();
		data.isPrivate = $("#editSpaceModal").find('input[name="isPrivate"]').is(":checked");
		data.articleHidden = $("#editSpaceModal").find('input[name="articleHidden"]').is(":checked");
		data.isDefault = $("#editSpaceModal").find('input[name="isDefault"]').is(":checked");
		$.ajax({
			type : "post",
			url : basePath+"/mgr/space/update",
			data : JSON.stringify(data),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if(data.success){
					success(data.message);
					setTimeout(function(){
						window.location.reload();
					},500);
				} else {
					error(data.message);
				}
			},
			complete:function(){
				$("#update").prop("disabled",false);
			}
		});
	});
	$("button[data-cc-action]").click(function(){
		var action = $(this).attr('data-cc-action');
		switch(action){
		case 'add':
		case 'update':
			var commentConfig = {};
			commentConfig.allowComment = $("#allowComment").prop("checked");
			commentConfig.commentMode = $("#commentMode").val();
			commentConfig.asc = $("#commentSort").val();
			commentConfig.allowHtml = $("#allowHtml").prop("checked");
			commentConfig.limitSec = $("#limitSec").val();
			commentConfig.limitCount = $("#limitCount").val();
			commentConfig.check = $("#check").prop("checked");
			$.ajax({
				type : "post",
				url : basePath+'/mgr/space/commentConfig/update?spaceId='+$("#spaceId").val(),
	            contentType:"application/json",
				data : JSON.stringify(commentConfig),
				success : function(data){
					if(data.success){
						success(data.message);
						setTimeout(function(){
							window.location.reload();
						},500);
					} else {
						error(data.message);
					}
				},
				complete:function(){
				}
			});
			break;
		case "delete":
			$.ajax({
				type : "post",
				url : basePath+'/mgr/space/commentConfig/delete?spaceId='+$("#spaceId").val(),
	            contentType:"application/json",
				data : {},
				success : function(data){
					if(data.success){
						success(data.message);
						setTimeout(function(){
							window.location.reload();
						},500);
					} else {
						error(data.message);
					}
				},
				complete:function(){
				}
			});

			break;
		}
	})
});
function editCommentConfig(id){
	clearTip();
	$("#spaceId").val(id);
	$("#editCommentConfigForm")[0].reset();
	var modal = $(this);
	$.get(basePath+'/mgr/space/get/'+id,{},function(data){
		if(data.success){
			data = data.data;
			if(!data.commentConfig){
				//没有配置评论
				$("button[data-cc-action]").hide();
				$("button[data-cc-action='add']").show();
			}else{
				var cc = data.commentConfig;
				if(cc.allowComment)
					$("#allowComment").prop("checked",true);
				$("#commentMode").val(cc.commentMode);
				$("#commentSort").val(cc.asc+"")
				if(cc.allowHtml)
					$("#allowHtml").prop("checked",true)
				$("#limitSec").val(cc.limitSec);
				$("#limitCount").val(cc.limitCount);
				if(cc.check)
					$("#check").prop("checked",true);
				$("button[data-cc-action]").hide();
				$("button[data-cc-action='delete']").show();
				$("button[data-cc-action='update']").show();
			}
			$("#editCommentConfigModal").modal('show');
		}else{
			bootbox.alert(data.message);
		}
	});
}