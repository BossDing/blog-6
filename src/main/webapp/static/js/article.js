var max = 3;
var login = $("#login").val() == 'true';
			var flag = false;
			var oauthLogin = $("#oauthLogin").val() == "true";
			var asc = false;
			var tree = $("#commentMode").val() == 'TREE';
			$(document).ready(function(){
				editormd.katexURL  = {
			        css : basePath+"/static/editor/markdown/lib/katex.min",
			        js  : basePath+"/static/editor/markdown/lib/katex.min"
			    };
				editormd.markdownToHTML("markdown-rendered", {
                    emoji           : true,
                    taskList        : true,
                    tex             : true,  // 默认不解析
                    flowChart       : true,  // 默认不解析
                    sequenceDiagram : true,  // 默认不解析
                    tocm : true
                });
				queryComments(0);
				if(window.sessionStorage){
					var articleId = "article-"+$("#articleId").val();
					var item = window.sessionStorage.getItem(articleId);
					if(!item || item == null){
						$.post($("#urlPrefix").val()+"/article/hit/"+$("#articleId").val(),{},function(data){
							if(data.success){
								window.sessionStorage.setItem(articleId,"1");
							}
						})
					}
				};
				$("#add-comment-btn").click(function(){
					if(flag){
						return ;
					}
					flag = true;
					$.ajax({
						type : "post",
						url : actPath+"/article/"+$("#articleId").val()+"/addComment",
						data : JSON.stringify({content:$('#comment-box').val()}),
						dataType : "json",
						contentType : 'application/json',
						success : function(data){
							if(data.success){
								$("#comment-box").val("");
								bootbox.alert("评论成功");
								var html = buildHtml(data.data.parent,data.data);
								if(asc){
									$("#comments-container").append(html);
								}else{
									$("#comments-container").prepend(html);
								}
							}else{
								bootbox.alert(data.message);
							}
						},
						complete:function(){
							flag = false;
						}
					});
				})
			});
			function queryComments(page){
				$.get(actPath+'/article/'+$("#articleId").val()+'/comment/list',{currentPage:page},function(data){
					asc = data.param.asc;
					var html = '';
					for(var i=0;i<data.datas.length;i++){
						var c = data.datas[i];
						if(tree){
							html += buildHtml(null,c);
						}else{
							html += buildHtml(c.parent,c);
						}
					}
						var pageBarHtml = '';
						if(data.totalPage > 1){
							pageBarHtml += '<div>';
							pageBarHtml += '<ul class="pagination">';
							for(var i=data.listbegin;i<=data.listend-1;i++){
								if(i == data.currentPage){
									pageBarHtml += '<li class="active">';
								}else{
									pageBarHtml += '<li>';
								}
								pageBarHtml += '<a href="###" onclick="queryComments(\''+i+'\')">'+i+'</a>';
								pageBarHtml += '</li>';
							}
							pageBarHtml += '</ul>';
							pageBarHtml += '</div>';
						}
					$("#comments-container").html(html);
					$("#page-bar").html(pageBarHtml);
	    		});
			}
			function toReply(parent){
				bootbox.dialog({
					title : '回复一个评论',
					message : '<div id="reply-tip"></div><textarea class="form-control" id="reply-box" placeholder="对TA说点什么"></textarea>',
					buttons : {
						success : {
							label : "确定",
							className : "btn-success",
							callback:function(e){
								if(flag){
									return false;
								}
								var data = {};
								data.parent = {"id":parent};
								data.content = $('#reply-box').val();
								$("#reply-tip").html('')
								flag = true;
								var sign = false;
								$.ajax({
									type : "post",
									url : actPath+"/article/"+$("#articleId").val()+"/addComment",
									data : JSON.stringify(data),
									async:false,
									dataType : "json",
									contentType : 'application/json',
									success : function(data){
										if(data.success){
											var c = data.data;
											var p = c.parent;
											var html = buildHtml(p,c);
											if(!tree || c.parents.length > max){
												if(tree){
													$("#comment-"+parent).after(html);
												}else{
													$("#comments-container").append(html);
												}
											}else if(tree){
									   			$("#comment-"+parent).find(".media-body:first").append(html);
									   		}
											sign = true;
										}else{
											$("#reply-tip").html('<div class="alert alert-danger">'+data.message+'</div>');
										}
									},
									complete:function(){
										flag = false;
									}
								});
								return sign;
							}
						}
					}
				});
			}
			 function buildHtml(p,c){
				 var html = '';
				 if(tree){
					  html += '<div class="media" id="comment-'+c.id+'" >';
					 html += '<a class="pull-left">';
					 html += '<img class="media-object"  src="'+c.user.avatar+'" data-holder-rendered="true" style="width: 64px; height: 64px;">';
					html += '</a>';
					html += '<div class="media-body">';
					 var username = getUsername(c);
					var user = '<strong>'+username+'</strong>';
					var p_username= getUsername(p); 
					var reply = (p == null || !p)?"":'<span class="glyphicon glyphicon-share-alt" aria-hidden="true"></span>&nbsp;&nbsp;'+p_username;
					html += '<h5 class="media-heading">'+user+'&nbsp;&nbsp;&nbsp;'+reply+'</h5>';
					html += c.content;
					html += '<h5>'+new Date(c.commentDate).format('yyyy-mm-dd HH:MM:ss')+'&nbsp;&nbsp;&nbsp;';
					if(login){
						if(!c.user.admin && c.user.status == 'NORMAL')
							html += '<a href="###" onclick="disable(\''+c.user.id+'\')" style="margin-right:8px"><span class="glyphicon glyphicon-ban-circle" aria-hidden="true"></span></a>';
						if(!c.user.admin && c.user.status == 'DISABLED')
							html += '<a href="###" onclick="enable(\''+c.user.id+'\')" style="margin-right:8px"><span class="glyphicon glyphicon-ok-circle" aria-hidden="true"></span></a>';
						html += '<a href="###" onclick="remove(\''+c.id+'\')" style="margin-right:8px"><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>';
					 }
					 if(oauthLogin)
						html += '<a href="###" onclick="toReply(\''+c.id+'\')"><span class="glyphicon glyphicon-comment" aria-hidden="true"></span></a>';
					html += '</h5>';
					if(c.parents.length < max && c.children.length > 0){
						for(var i=0;i<c.children.length;i++){
							html += buildHtml(c,c.children[i]);
						}
					}
					html += '</div>';
					html += '</div>';
					if(c.parents.length >= max && c.children.length > 0){
						for(var i=0;i<c.children.length;i++){
							html += buildHtml(c,c.children[i]);
						}
					}
					return html;
				 }else{
					  html += '<div class="media" id="comment-'+c.id+'" data-p="'+((!c.parent || c.parent==null)?'':c.parent.id)+'">';
					 html += '<a class="pull-left">';
					 html += '<img class="media-object"  src="'+c.user.avatar+'" data-holder-rendered="true" style="width: 64px; height: 64px;">';
					html += '</a>';
					html += '<div class="media-body">';
					 var username = getUsername(c);
					var user = '<strong>'+username+'</strong>';
					var p_username= getUsername(p); 
					var reply = (p == null || !p)?"":'<span class="glyphicon glyphicon-share-alt" aria-hidden="true"></span>&nbsp;&nbsp;'+p_username;
					html += '<h5 class="media-heading">'+user+'&nbsp;&nbsp;&nbsp;'+reply+'</h5>';
					html += c.content;
					html += '<h5>'+new Date(c.commentDate).format('yyyy-mm-dd HH:MM:ss')+'&nbsp;&nbsp;&nbsp;';
					if(login){
						if(!c.user.admin && c.user.status == 'NORMAL')
							html += '<a href="###" onclick="disable(\''+c.user.id+'\')" style="margin-right:8px"><span class="glyphicon glyphicon-ban-circle" aria-hidden="true"></span></a>';
						if(!c.user.admin && c.user.status == 'DISABLED')
							html += '<a href="###" onclick="enable(\''+c.user.id+'\')" style="margin-right:8px"><span class="glyphicon glyphicon-ok-circle" aria-hidden="true"></span></a>';
						html += '<a href="###" onclick="remove(\''+c.id+'\')" style="margin-right:8px"><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>';
					 }
					 if(oauthLogin)
							html += '<a href="###" onclick="toReply(\''+c.id+'\')"><span class="glyphicon glyphicon-comment" aria-hidden="true"></span></a>';
					
					html += '</h5>';
					html += '</div>';
					html += '</div>';
					return html;
				 }
				
			 }
			 
			 function getUsername(c){
				 if(c == null || !c){
					 return '';
				 }
				 var username = '';
				 if(c.user.admin){
					username = '<span class="glyphicon glyphicon-user" style="color:red" title="管理员"></span>&nbsp;'+c.user.nickname
				 }else{
					 username = c.user.nickname
				 }
				 return username;
			 }
			 
			 function remove(id){
				 bootbox.confirm("确定要删除吗？",function(result){
						if(result){
							$.ajax({
			    				type : "post",
			    				url : rootPath+"/mgr/comment/delete?id="+id,
			    	            contentType:"application/json",
			    				data : {},
			    				xhrFields: {
			    	                withCredentials: true
			    	            },
			    	            crossDomain: true,
			    				success : function(data){
			    					if(data.success){
				    					$("#comment-"+id).remove();
				    					if(!tree){
				    						$("[data-p]").each(function(){
				    							var me = $(this);
				    							if(me.attr("data-p")==id){
				    								me.remove();
				    							}
				    						})
				    					}
			    					}else{
			    						bootbox.alert(data.message);
			    					}
			    				},
			    				complete:function(){
			    				}
			    			});
						}
					});
			 }
			 
			 function disable(id){
				 bootbox.confirm("确定要禁用吗？", function(result) {
						if (result) {
							$.ajax({
			    				type : "post",
			    				url : rootPath + "/mgr/oauth/disable",
			    				data : {id : id},
			    				xhrFields: {
			    	                withCredentials: true
			    	            },
			    	            crossDomain: true,
			    				success : function(data){
			    					if(data.success){
										 bootbox.confirm("是否要删除该用户这篇文章下的所有评论？", function(result) {
												if(result){
													$.ajax({
									    				type : "post",
									    				url : rootPath + "/mgr/comment/delete",
									    				data : {userId : id,articleId:$("#articleId").val()},
									    				xhrFields: {
									    	                withCredentials: true
									    	            },
									    	            crossDomain: true,
									    				success : function(data){
									    					bootbox.alert(data.message);
									    					if(data.success){
																queryComments(0);
															}
									    				},
									    				complete:function(){
									    				}
									    			});
												}else{
													window.location.reload();
												}
										 });
									}else{
										bootbox.alert(data.message);
									}
			    				},
			    				complete:function(){
			    				}
			    			});
						}
					});
			 }
			 
			 function enable(id){
				 bootbox.confirm("确定要解禁吗？", function(result) {
						if (result) {
							$.ajax({
			    				type : "post",
			    				url : rootPath + "/mgr/oauth/enable",
			    				data : {id : id},
			    				xhrFields: {
			    	                withCredentials: true
			    	            },
			    	            crossDomain: true,
			    				success : function(data){
			    					bootbox.alert(data.message);
									if(data.success){
										setTimeout(function(){
											window.location.reload();
										},500)
									}
			    				},
			    				complete:function(){
			    				}
			    			});
						}
					});
			 }