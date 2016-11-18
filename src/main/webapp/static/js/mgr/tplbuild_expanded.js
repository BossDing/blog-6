function preview() {
		var page = {"tpl":editor.getValue()};
		page.tpls = fragments;
		page.name = "test";
		var id = $("#pageId").val();
		page.id = id;
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/expanded/preview',
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
		var page = {"tpl":editor.getValue()};
		page.tpls = fragments;
		var id = $("#pageId").val();
		page.id = id;
		page.name = $("#name").val();
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/expanded/build',
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