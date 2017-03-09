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
		page.allowComment = $("#allowComment").prop("checked");
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
		page.name=$("#name").val();
		page.description=$("#description").val();
		page.allowComment = $("#allowComment").prop("checked");
		page.registrable = $("#registrable").prop("checked");
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/user/preview',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
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
				} else {
					showError(data);
				}
			},
			complete:function(){
			}
		});
	}