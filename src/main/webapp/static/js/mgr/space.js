$(document).ready(function() {
	$("#spaceModal").on("show.bs.modal",function(){
		clearTip();
		$(this).find("form")[0].reset();
	})
	$("#editSpaceModal").on("show.bs.modal",function(event){
		clearTip();
		$(this).find("form")[0].reset();
		var a = $(event.relatedTarget);
		var tr = a.parent().parent();
		var modal = $(this)
		modal.find('.modal-body input[name="name"]').val($(tr.find('td')[0]).find('a').attr("data-original-title"));
		modal.find('.modal-body input[name="alias"]').val($(tr.find('td')[1]).find('a').attr("data-original-title"));
		var status = $(tr.find('td')[3]).text();
		modal.find('.modal-body select[name="status"]').val($.trim(status) == '正常' ? 'NORMAL' : 'DISABLE');
		var isDefault = $(tr.find('td')[4]).text();
		modal.find('.modal-body input[name="isDefault"]').prop("checked",$.trim(isDefault) == "是");
		modal.find('.modal-body input[name="id"]').val(a.attr("data-id"));
		var hasLock = $(tr.find('td')[5]).text();
		if($.trim(hasLock) == "是"){
			var lockId = $(tr.find('td')[5]).attr("data-lockId");
			modal.find('.modal-body select[name="lockId"]').val(lockId)
		} else {
			modal.find('.modal-body select[name="lockId"]').val('');
		}
		
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
});