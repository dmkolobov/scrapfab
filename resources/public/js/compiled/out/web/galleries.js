// Compiled by ClojureScript 1.9.89 {}
goog.provide('web.galleries');
goog.require('cljs.core');
/**
 * Return the ideal number of rows to fill the container with items.
 */
web.galleries.ideal_row_count = (function web$galleries$ideal_row_count(p__7378,aspects,rows_per_screen){
var vec__7382 = p__7378;
var width = cljs.core.nth.call(null,vec__7382,(0),null);
var height = cljs.core.nth.call(null,vec__7382,(1),null);
var container = vec__7382;
var ideal_height = (height / rows_per_screen);
return Math.round(((ideal_height * cljs.core.reduce.call(null,cljs.core._PLUS_,aspects)) / width));
});
web.galleries.selector = (function web$galleries$selector(key_fn,coll){
var cache = cljs.core.atom.call(null,cljs.core.group_by.call(null,key_fn,coll));
return ((function (cache){
return (function (k){
var item = cljs.core.first.call(null,cljs.core.get.call(null,cljs.core.deref.call(null,cache),k));
cljs.core.swap_BANG_.call(null,cache,cljs.core.update,k,cljs.core.rest);

return item;
});
;})(cache))
});
/**
 * Items are partitioned into sequences so that the sum of the aspect ratios
 *   in each sequence is roughly equal. Returns a sequence of sequences containing
 *   [id aspect-ratio] tuples.
 * 
 *   Arguments:
 * 
 *   - container       : a [width height] tuple representing the dimensions of the layout container.
 *   - items           : a sequence of [id aspect-ratio] tuples.
 *   - rows-per-screen : the ideal number of rows per screen
 */
web.galleries.perfect_layout = (function web$galleries$perfect_layout(container,items,rows_per_screen){
var aspects = cljs.core.map.call(null,cljs.core.last,items);
var partitions = lpartition(cljs.core.clj__GT_js.call(null,cljs.core.map.call(null,((function (aspects){
return (function (p1__7385_SHARP_){
return ((100) * p1__7385_SHARP_);
});})(aspects))
,aspects)),web.galleries.ideal_row_count.call(null,container,aspects,rows_per_screen));
cljs.core.map.call(null,cljs.core.partial.call(null,cljs.core.map,web.galleries.selector.call(null,((function (aspects,partitions){
return (function (p1__7386_SHARP_){
return ((100) * cljs.core.last.call(null,p1__7386_SHARP_));
});})(aspects,partitions))
,items)));

return partitions;
});
web.galleries.sum_row_aspects = cljs.core.comp.call(null,(function (p1__7387_SHARP_){
return cljs.core.reduce.call(null,cljs.core._PLUS_,p1__7387_SHARP_);
}),cljs.core.map.call(null,cljs.core.last));
web.galleries.row_height = (function web$galleries$row_height(row_width,items){
return (web.galleries.sum_row_aspects.call(null,items) / row_width);
});
web.galleries.do_scale_layout = (function web$galleries$do_scale_layout(width,layout,gap){
var G__7407 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(0),(0)], null);
var vec__7408 = G__7407;
var x = cljs.core.nth.call(null,vec__7408,(0),null);
var y = cljs.core.nth.call(null,vec__7408,(1),null);
var scaled_row = cljs.core.PersistentVector.EMPTY;
var height = web.galleries.row_height.call(null,width,cljs.core.first.call(null,layout));
var row = cljs.core.first.call(null,layout);
var rows = cljs.core.rest.call(null,layout);
var scaled_layout = cljs.core.PersistentVector.EMPTY;
var G__7407__$1 = G__7407;
var scaled_row__$1 = scaled_row;
var height__$1 = height;
var row__$1 = row;
var rows__$1 = rows;
var scaled_layout__$1 = scaled_layout;
while(true){
var vec__7411 = G__7407__$1;
var x__$1 = cljs.core.nth.call(null,vec__7411,(0),null);
var y__$1 = cljs.core.nth.call(null,vec__7411,(1),null);
var scaled_row__$2 = scaled_row__$1;
var height__$2 = height__$1;
var row__$2 = row__$1;
var rows__$2 = rows__$1;
var scaled_layout__$2 = scaled_layout__$1;
if(cljs.core.seq.call(null,row__$2)){
var vec__7414 = row__$2;
var seq__7415 = cljs.core.seq.call(null,vec__7414);
var first__7416 = cljs.core.first.call(null,seq__7415);
var seq__7415__$1 = cljs.core.next.call(null,seq__7415);
var vec__7417 = first__7416;
var id = cljs.core.nth.call(null,vec__7417,(0),null);
var aspect_ratio = cljs.core.nth.call(null,vec__7417,(1),null);
var row_SINGLEQUOTE_ = seq__7415__$1;
var item_width = (aspect_ratio * height__$2);
var item_layout = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [id,item_width,height__$2,x__$1,y__$1], null);
var G__7420 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [((x__$1 + item_width) + gap)], null);
var G__7421 = cljs.core.conj.call(null,scaled_row__$2,item_layout);
var G__7422 = height__$2;
var G__7423 = row_SINGLEQUOTE_;
var G__7424 = rows__$2;
var G__7425 = scaled_layout__$2;
G__7407__$1 = G__7420;
scaled_row__$1 = G__7421;
height__$1 = G__7422;
row__$1 = G__7423;
rows__$1 = G__7424;
scaled_layout__$1 = G__7425;
continue;
} else {
if(cljs.core.seq.call(null,rows__$2)){
var G__7426 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(0),((y__$1 + height__$2) + gap)], null);
var G__7427 = cljs.core.PersistentVector.EMPTY;
var G__7428 = web.galleries.row_height.call(null,width,cljs.core.first.call(null,rows__$2));
var G__7429 = cljs.core.first.call(null,rows__$2);
var G__7430 = cljs.core.rest.call(null,rows__$2);
var G__7431 = cljs.core.conj.call(null,scaled_layout__$2,scaled_row__$2);
G__7407__$1 = G__7426;
scaled_row__$1 = G__7427;
height__$1 = G__7428;
row__$1 = G__7429;
rows__$1 = G__7430;
scaled_layout__$1 = G__7431;
continue;
} else {
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj.call(null,scaled_layout__$2,scaled_row__$2),width,(y__$1 + height__$2)], null);

}
}
break;
}
});
/**
 * Returns a sequence of sequences containing [id [x y] [width height]] tuples.
 * 
 *   Arguments:
 * 
 *   - container : a [width height] tuple representing the dimensions of the layout container.
 *   - layout    : sequence of sequences containing [id aspect-ratio] tuples.
 *   - gap       : a number representing the number of pixels of whitespace
 *              to keep between inner layout items.
 */
web.galleries.scale_layout = (function web$galleries$scale_layout(container,layout,gap){
return web.galleries.do_scale_layout.call(null,cljs.core.first.call(null,container),layout,gap);
});

//# sourceMappingURL=galleries.js.map?rel=1483091636507