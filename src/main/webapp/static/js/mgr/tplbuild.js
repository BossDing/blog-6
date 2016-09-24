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
			$("#grab_url").val("")
		});
		$("#allWidgetsModal").on("show.bs.modal", function() {
			showSysWidget();
		})
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
			var html = '';
			var id = $(e.target).attr('id');
			switch(id){
			case "sys-tab":
				showSysWidget();
				break;
			case "user-tab":
				showUserWidget(1)
				break;
			}
		})
		$("#widgetsModal").on("shown.bs.modal",function(){
			$("#widgetsModal .modal-body").html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
			var data = {"target":$("#target").val(),"tpl":editor.getValue()};
			var id = $("#pageId").val();
			if(id != null && $.trim(id) != ''){
				data.id = id;
			}
			var space = $("#space").val();
			if(space != null && $.trim(space) != ''){
				data.space = {"id":space}
			}
			$.ajax({
				type : "post",
				url : basePath+"/mgr/page/sys/parseWidget",
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
	
	
	function addWidget(name){
		editor.insertValue('<widget name="'+name+'"></widget>');
		$("#allWidgetsModal").modal('hide')
	}
	
	function showSysWidget(){
		var html = '';
		$.get(basePath+"/mgr/widget/sys/all",{},function(data){
			if(!data.success){
				bootbox.alert(data.message);
				return ;
			}
			data = data.data;
			html += '<div class=" table-responsive" style="margin-top:10px">';
			html += '<table class="table">';
			for(var i=0;i<data.length;i++){
				html += '<tr>';
				html += '<td>'+data[i].name+'</td>';
				html += '<td><a onclick="addWidget(\''+data[i].name+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			$('[aria-labelledby="sys-tab"]').html(html);
		});
	}
	function showUserWidget(i){
		var html = '';
		$.get(basePath+"/mgr/widget/user/list",{"currentPage":i},function(data){
			if(!data.success){
				bootbox.alert(data.message);
				return ;
			}
			var page = data.data;
			html += '<div class=" table-responsive" style="margin-top:10px">';
			html += '<table class="table">';
			for(var i=0;i<page.datas.length;i++){
				html += '<tr>';
				html += '<td>'+page.datas[i].name+'</td>';
				html += '<td><a onclick="addWidget(\''+page.datas[i].name+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			
			if(page.totalPage > 1){
				html += '<div>';
				html += '<ul class="pagination">';
				for(var i=page.listbegin;i<=page.listend-1;i++){
					html += '<li>';
					html += '<a href="###" onclick="showUserWidget(\''+i+'\')" >'+i+'</a>';
					html += '</li>';
				}
				html += '</ul>';
				html += '</div>';
			}
			$('[aria-labelledby="user-tab"]').html(html);
		});
	}
	
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
		var page = {"target":$("#target").val(),"tpl":editor.getValue()};
		page.tpls = widgets;
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
			url : basePath + '/mgr/page/sys/preview',
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
								editor.setValue(widget.tpl);
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
		var space = $("#space").val();
		if(space != null && $.trim(space) != ''){
			page.space = {"id":space}
		}
		$.ajax({
			type : "post",
			url : basePath + '/mgr/page/sys/build',
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
							widgetTplEditor.setValue(data.template.tpl)
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
							editor.setValue(widget.tpl);
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
			url : basePath+"/mgr/page/SYSTEM/"+pageId+"/widget/"+widgetType+"/"+widgetId+"/delete",
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