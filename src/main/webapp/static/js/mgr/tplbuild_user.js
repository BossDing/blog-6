function preview() {
		var page = {"tpl":editor.getValue()};
		page.tpls = fragments;
		var space = $("#spaceSelect").val();
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
			url : basePath + '/mgr/template/page/preview',
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
						var win = window.open(basePath + '/mgr/template/preview',
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
		$.ajax({
			type : "post",
			url : basePath + '/mgr/template/page/preview',
			data : JSON.stringify(page),
			dataType : "json",
			contentType : 'application/json',
			success : function(data){
				if (data.success) {
					$.ajax({
						type : "post",
						url : basePath + '/mgr/template/page/build',
						data : JSON.stringify(page),
						dataType : "json",
						contentType : 'application/json',
						success : function(data){
							if (data.success) {
								bootbox.alert(data.message);
								setTimeout(function(){
									window.location.href = basePath + '/mgr/template/page/index';
								}, 500);
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