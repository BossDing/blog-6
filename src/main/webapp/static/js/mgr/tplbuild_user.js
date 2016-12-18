$(document).ready(function(){
	$.get(basePath + '/mgr/lock/all',{},function(data){
		var oldLock = $("#oldLock").val();
		if(data.success){
			var locks = data.data;
			if(locks.length > 0){
				var html = '<div class="row"><div style="margin-top: 10px"><div class="col-md-2">';
				html += '<label for="lockId" class="control-label">锁:</label> ';
				html += '</div>';
				html += '<div class="col-md-10">';
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
				html += '</div></div>'
				$("#lock_container").html(html);
				$("#lockId").val($("#pageLockId").val());
			}
		}else{
			console.log(data.data);
		}
	});
})
function preview() {
		var page = {"tpl":editor.getValue()};
		page.tpls = fragments;
		var space = $("#space").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
		}
		page.name="test";
		page.description="";
		page.alias = "test";
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/user/preview',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					var win = window.open(basePath + '/mgr/page/preview',
							'_blank');
					win.focus();
				} else {
					showError(data);
				}
			},
			complete:function(){
			}
		});
	}
	function save() {
		var page = {"target":$("#target").val(),"tpl":editor.getValue()};
		page.tpls = fragments;
		var space = $("#spaceSelect").val();
		if(space != ''){
			page.space = {"id":space}
		}
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
		}
		if($.trim($("#alias").val()) != ''){
			page.alias = $.trim($("#alias").val());
		}
		if($("#lockId").val() != ""){
			page.lockId = $("#lockId").val();
		}
		page.name=$("#name").val();
		page.description=$("#description").val();
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/user/build',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					bootbox.alert(data.message);
				} else {
					showError(data);
				}
			},
			complete:function(){
			}
		});
	}