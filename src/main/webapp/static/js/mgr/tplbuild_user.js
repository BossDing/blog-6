var editor;
var widgetTplEditor;
var widgets = [];
var _tpls = [];
	$(document).ready(function() {
		editor = editormd("editor", {
			width : "100%",
			height : $(window).height() * 0.8,
			 watch            : false,
             toolbar          : false,
             codeFold         : true,
             searchReplace    : true,
             theme            : "default",
             mode             : "text/html",
             path : basePath + '/static/editor/markdown/lib/'
		});
		widgetTplEditor = editormd("widgetTplEditor", {
			width : "100%",
			height : 600,
			 watch            : false,
             toolbar          : false,
             codeFold         : true,
             searchReplace    : true,
             theme            : "default",
             mode             : "text/html",
             path : basePath + '/static/editor/markdown/lib/'
		});
		$("#loadingModal").on("hidden.bs.modal", function() {
			clearTip();
			$("#loadingModal img").show();
		})
		$("#grabModal").on("hidden.bs.modal", function() {
			$("#loadingModal img").show();
		}).on("show.bs.modal", function() {
			clearTip();
			$("#grab_url").val("")
		});
		
		$("#widgetsModal").on("shown.bs.modal",function(){
			clearTip();
			$("#widgetsModal .modal-body").html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
			var data = {"tpl":editor.getValue()};
			var id = $("#pageId").val();
			if(id != null && $.trim(id) != ''){
				data.id = id;
			}
			var space = $("#space").val();
			if(space != null && $.trim(space) != ''){
				data.space = {"id":space}
			}
			data.name="test";
			data.description="";
			page.alias = "test";
			$.ajax({
				type : "post",
				url : basePath+"/mgr/page/user/parseWidget",
				data : JSON.stringify(data),
				dataType : "json",
				contentType : 'application/json',
				success : function(data){
					if(data.success){
						var tpls = data.data;
						if(tpls.length > 0){
							var html = '<div class="panel panel-default"> ';
							html += ' <div class="table-responsive"> ';
							html += '  <table class="table"> ';
							html += '  <tbody> ';
							_tpls = tpls;
							for(var i=0;i<tpls.length;i++){
								var tpl = tpls[i];
								html += '  <tr> ';
								html += ' <td><span>'+tpl.widget.name+'</span></td>'; 
								var id = $("#pageId").val();
								if(id != null && $.trim(id) != '' && tpl.widget.id && tpl.widget.id != ""){
									html += ' <td><a href="###" onclick="revert(\''+id+'\',\''+tpl.widget.id+'\',\''+tpl.widget.type+'\')" style="margin-right:10px" title="还原"><span class="glyphicon glyphicon-repeat" aria-hidden="true"></span></a><a href="###" onclick="showWidget($(this))" data-index="'+i+'"><span class="glyphicon glyphicon-edit" aria-hidden="true"></span></a></td>'; 
								} else {
									html += ' <td><a href="###" data-index="'+i+'" onclick="showWidget($(this))"><span class="glyphicon glyphicon-edit" aria-hidden="true"></span></a></td>'; 
								}
								html += ' </tr> ';
							}
							html += '   </tbody> ';
							html += '   </table> ';
							html += '    </div> ';
							html += '</div>';
							$("#widgetsModal .modal-body").html(html);
						} else {
							$("#widgetsModal .modal-body").html('<div class="alert alert-info">没有挂件</div>')
						}
					} else {
						$("#widgetsModal .modal-body").html('<div class="alert alert-danger">'+data.message+'</div>');
					}
				},
				complete:function(){
					$("#create").prop("disabled",false);
				}
			});
		}).on("hidden.bs.modal", function() {
			$("#widgetsModal .modal-body").html("")
		})
		$("#widgetTplEditModal").on("shown.bs.modal",function(){
			clearTip();
			widgetTplEditor.resize();
		}).on("hidden.bs.modal",function(){
			
		})
	});
	function showWidget(o){
		var index= o.attr("data-index");
		var tpl = _tpls[index];
		var widget = tpl.widget;
		for(var i=0;i<widgets.length;i++){
			var _widget = widgets[i].widget;
			if(widget.name == _widget.name){
				tpl.tpl = widgets[i].tpl;
			}
		}
		//系统挂件
		$("#widgetsModal").modal("hide");
		$("#widgetTplEditModal").modal("show");
		widgetTplEditor.setValue(tpl.tpl);
		$("#tpl-save-btn").off("click").on("click",function(){
			var btn  = $(this);
			var data = {tpl:widgetTplEditor.getValue()};
			data.widget = widget;
			var exists = false;
			for(var i=0;i<widgets.length;i++){
				var tpl = widgets[i];
				if(tpl.widget.name == widget.name){
					tpl.tpl = data.tpl;
					exists = true;
				}
			}
			if(!exists){
				widgets.push(data);
			}
			success("保存成功");
			setTimeout(function(){
				$("#widgetTplEditModal").modal("hide");
				$("#widgetsModal").modal("show");
			},500)
		});
	}
	function preview() {
		var page = {"tpl":editor.getValue()};
		page.tpls = widgets;
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
					if(data.message){
						bootbox.alert(data.message);
						return ;
					}
					data = data.data;
					if (data.line) {
						if(data.template && data.template.widget){
							var widget = data.template.widget;
							$("#widgetTplEditModal").modal("show");
							widgetTplEditor.setValue(data.template.tpl);
							if (data.expression) {
								error("第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								error("第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							$("#tpl-save-btn").off("click").on("click",function(){
								var btn  = $(this);
								var data = {tpl:widgetTplEditor.getValue()};
								data.widget = widget;
								var exists = false;
								for(var i=0;i<widgets.length;i++){
									var tpl = widgets[i];
									if(tpl.widget.name == widget.name){
										tpl.tpl = data.tpl;
										exists = true;
									}
								}
								if(!exists){
									widgets.push(data);
								}
								success("保存成功");
								setTimeout(function(){
									$("#widgetTplEditModal").modal("hide");
									$("#widgetsModal").modal("show");
								},500)
							});
						} else {
							var html = '';
							if (data.templateName) {
								html += '模板:' + data.templateName + ":"
							}
							if (data.expression) {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							if (data.tpl){
								editor.setValue(data.tpl)
							}
						}
					}
				}
			},
			complete:function(){
			}
		});
	}
	function save() {
		var page = {"target":$("#target").val(),"tpl":editor.getValue()};
		page.tpls = widgets;
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
					if(data.message){
						bootbox.alert(data.message);
						return ;
					}
					data = data.data;
					if (data.line) {
						if(data.template && data.template.widget){
							var widget = data.template.widget;
							$("#widgetTplEditModal").modal("show");
							$("#widgetTplEditor").setValue(data.template.tpl);
							if (data.expression) {
								error("第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								error("第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							$("#tpl-save-btn").off("click").on("click",function(){
								var btn  = $(this);
								var data = {tpl:widgetTplEditor.getValue()};
								data.widget = widget;
								var exists = false;
								for(var i=0;i<widgets.length;i++){
									var tpl = widgets[i];
									if(tpl.widget.name == widget.name){
										tpl.tpl = data.tpl;
										exists = true;
									}
								}
								if(!exists){
									widgets.push(data);
								}
								success("保存成功");
								setTimeout(function(){
									$("#widgetTplEditModal").modal("hide");
									$("#widgetsModal").modal("show");
								},500)
							});
						} else {
							var html = '';
							if (data.templateName) {
								html += '模板:' + data.templateName + ":"
							}
							if (data.expression) {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列，表达式：" + data.expression
										+ "发生错误")
							} else {
								bootbox.alert(html + "第" + data.line + "行,"
										+ data.col + "列发生错误")
							}
							if (data.tpl){
								editor.setValue(data.tpl)
							}
						}
					}
				}
			},
			complete:function(){
			}
		});
	}
	
	function revert(pageId,widgetId,widgetType){
		$("#widgetsModal").modal('hide');
		$.ajax({
			type : "post",
			 async: false,
			url : basePath+"/mgr/page/USER/"+pageId+"/widget/"+widgetType+"/"+widgetId+"/delete",
			data : {},
			success : function(data){
				if(data.success){
					success(data.message);
					setTimeout(function(){
						$("#widgetsModal").modal('show');
					},500);
					flag = true;
				} else {
					error(data.message);
				}
			},
			complete:function(){
			}
		});
	}