$(function(){$(".rating").each(function(a,t){function e(){r.removeClass("rating__star--rated");for(var a=0;a<$(t).data("rating");a+=1)r.eq(a).addClass("rating__star--rated")}var r=$(t).find(".rating__star");e(),$(t).hover(null,function(){e()}),r.hover(function(a){r.removeClass("rating__star--rated");for(var t=0;t<$(a.currentTarget).index()+1;t+=1)r.eq(t).addClass("rating__star--rated")}),r.click(function(a){var n=$(a.currentTarget).index()+1;$(t).addClass("loading"),$.ajax({url:"/web/api/rating/rate",method:"PUT",data:JSON.stringify({pageId:$(t).data("page-id"),score:n}),contentType:"application/json",dataType:"json"}).then(function(a){$(t).data("rating",a.averageScore),e(),r.off("click mouseenter mouseleave"),$(t).find(".rating__total").text(a.totalScored),$(t).find(".rating__average").text(a.averageScore),$(t).removeClass("loading")}.bind(this))})})});