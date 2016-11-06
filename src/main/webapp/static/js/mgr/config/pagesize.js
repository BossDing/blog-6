$(document).ready(function() {
	$("#update").click(function() {
		var data = $("#pageSizeForm").serializeObject();
		$.ajax({
			type : "post",
			url : basePath + "/mgr/config/pagesize/update",
			data : JSON.stringify(data),
			dataType : "json",
			contentType : 'application/json',
			success : function(data) {
				if (data.success) {
					success(data.message);
					setTimeout(function() {
						window.location.reload();
					}, 500);
				} else {
					error(data.message);
				}
			},
			complete : function() {
				$("#update").prop("disabled", false);
			}
		});
	});
});