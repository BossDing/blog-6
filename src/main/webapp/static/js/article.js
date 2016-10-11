var max = 3;
var cp = 1;
var login = $("#login").val() == 'true';
			var flag = false;
			var oauthLogin = $("#oauthLogin").val() == "true";
			var asc = false;
			var tree = $("#commentMode").val() == 'TREE';
			$(document).ready(function(){
				queryComments(cp);
				if(window.sessionStorage && !login){
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
								if(data.data.status == 'CHECK'){
									bootbox.alert("评论成功，将会在审核通过后显示");
								}else{
									bootbox.alert("评论成功");
									queryComments(cp);
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
				cp = page;
				$.ajax({
				    type: 'get',
				    url: actPath+'/data/评论' ,
				    data: {"currentPage":page,"article":$("#articleId").val()} ,
				    beforeSend: function (xhr) {
				        xhr.setRequestHeader("X-Fragment", encodeURI("评论"));
				    },
				    success: function(data){
				    	if(!data.success){
							bootbox.alert(data.message);
							return ;
						}else{
							$("#comments-container").html(data.data);
						}
				    }
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
											queryComments(cp);
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
			 
			 function queryConversations(id){
				 $.get(actPath+'/article/'+$("#articleId").val()+'/comment/'+id+'/conversations',{},function(data){
					 if(!data.success){
						 bootbox.alert(data.message);
						 return;
					 }
					 data = data.data;
						var html = '';
						for(var i=0;i<data.length;i++){
							var c = data[i];
							var p = i == 0 ? null : data[i-1];
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
							html += '<h5>'+new Date(c.commentDate).format('yyyy-mm-dd HH:MM:ss')+'&nbsp;&nbsp;&nbsp;</h5>';
							html += '</div>';
							html += '</div>';
						}
						if($('#conversationsModal').length == 0){
							var modal = '<div class="modal " id="conversationsModal" tabindex="-1" role="dialog" >';
							modal += '<div class="modal-dialog" role="document">';
							modal += '<div class="modal-content">';
							modal += '<div class="modal-header">';
							modal += '<h4 class="modal-title">对话</h4>';
							modal += '</div>';
							modal += '<div class="modal-body" id="conversationsBody">';
							modal += '<div class="tip"></div>';
							modal += '</div>';
							modal += '<div class="modal-footer">';
							modal += '<button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>';
							modal += '</div>';
							modal += '</div>';
							modal += '</div>';
							modal += '</div>';
							$(modal).appendTo($('body'));
						}
						$('#conversationsBody').html(html);
						$('#conversationsModal').modal('show');
		    		});
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
			 
			 function removeComment(id){
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
			    						queryComments(cp);
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
			 
			 function checkComment(id){
				 bootbox.confirm("确定要审核通过吗？", function(result) {
						if (result) {
							$.ajax({
			    				type : "post",
			    				url : rootPath + "/mgr/comment/check?id="+id,
			    				data : {id : id},
			    				xhrFields: {
			    	                withCredentials: true
			    	            },
			    	            crossDomain: true,
			    				success : function(data){
			    					queryComments(cp);
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
			    						queryComments(cp);
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
			    					queryComments(cp);
			    				},
			    				complete:function(){
			    				}
			    			});
						}
					});
			 }