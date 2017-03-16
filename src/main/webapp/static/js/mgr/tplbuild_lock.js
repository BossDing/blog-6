function preview() {
		var page = {"lockType":$("#lockType").val(),"tpl":editor.getValue()};
		page.tpls = fragments;
		var space = $("#space").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		var id = $("#pageId").val();
		if(id != null && $.trim(id) != ''){
			page.id = id;
		}
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/lock/preview',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					if(data.url){
						var win = window.open(data.url,
							'_blank');
						win.focus();
					}else{
						var win = window.open(basePath + '/mgr/page/preview',
							'_blank');
						win.focus();
					}
				} else {
					showError(data);
				}
			},
			complete:function(){
			}
		});
	}

	function save() {
		var page = {"lockType":$("#lockType").val(),"tpl":editor.getValue()};
		page.tpls = fragments;
		var space = $("#space").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/lock/preview',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					$.ajax({
						type : "post",
						url : basePath + '/mgr/page/lock/build',
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