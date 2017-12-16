$(document).ready(
	function() {
		var spaceId = $("#pageFormSpaceId").val();
		if(spaceId != undefined){
			$("#query-space-checkbox").prop("checked",true);
			$("#space").val(spaceId).show();
		}
		var global = $("#pageFormGlobal").val();
		if(global && global == "true"){
			$("#query-global").prop("checked",true);
		}
		
		var callable = $("#pageFormCallable").val();
		if(callable && callable == "true"){
			$("#query-callable").prop("checked",true);
		}
		
		$("#query-space-checkbox").click(function(){
			$("#space").toggle();
		})
		$("#query-btn").click(function(){
			var form = "";
			$("#query-form").remove();
			form += '<form id="query-form" style="display:none" action="'+basePath+'/mgr/template/fragment/index" method="get">';
			var name = $.trim($("#query-name").val());
			if(name != ''){
				form += '<input type="hidden" name="name" value="'+name+'"/>';
			}
			if($("#query-space-checkbox").is(":checked")){
				form += '<input type="hidden" name="space.id" value="'+$("#space").val()+'"/>';
			}
			if($("#query-global").is(":checked")){
				form += '<input type="hidden" name="global" value="true"/>';
			}
			if($("#query-callable").is(":checked")){
				form += '<input type="hidden" name="callable" value="true"/>';
			}
			form += '</form>';
			$("body").append(form);
			$("#query-form").submit();
		})
		$("[data-page]").click(function(){
			var page = $(this).attr("data-page");
			$("#pageForm").find("input[name='currentPage']").val(page);
			$("#pageForm").submit();
		})
		$('[data-toggle="tooltip"]').tooltip();
	$("[data-action='remove']").click(function(){
		var me = $(this);
		bootbox.confirm("确定要删除吗?",function(result){
			if(!result){
				return ;
			}
			$.ajax({
				type : "post",
				url : basePath+"/mgr/template/fragment/delete",
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
	
	});
