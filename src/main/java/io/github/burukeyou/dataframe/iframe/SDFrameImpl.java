package io.github.burukeyou.dataframe.iframe;


import io.github.burukeyou.dataframe.iframe.function.ReplenishFunction;
import io.github.burukeyou.dataframe.iframe.function.SetFunction;
import io.github.burukeyou.dataframe.iframe.item.FI2;
import io.github.burukeyou.dataframe.iframe.item.FI3;
import io.github.burukeyou.dataframe.iframe.item.FI4;
import io.github.burukeyou.dataframe.iframe.support.*;
import io.github.burukeyou.dataframe.iframe.window.OverParam;
import io.github.burukeyou.dataframe.iframe.window.SupplierFunction;
import io.github.burukeyou.dataframe.util.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;


/**
 * @author caizhihao
 */
public class SDFrameImpl<T>  extends AbstractDataFrameImpl<T> implements SDFrame<T> {

    protected Stream<T> data;

    public SDFrameImpl(Stream<T> data) {
        this.data = data;
    }

    public SDFrameImpl(List<T> list) {
        this.data = list.stream();
        if (!list.isEmpty()){
            fieldList = buildFieldList(list.get(0));
        }
    }

    public <R> SDFrameImpl<R> read(List<R> list) {
        return new SDFrameImpl<>(list);
    }

    @Override
    public <R> SDFrame<R> from(Stream<R> data) {
        return new SDFrameImpl<>(data);
    }

    @Override
    public <R> SDFrame<R> map(Function<T, R> map) {
        return returnDF(stream().map(map).collect(toList()));
    }

    @Override
    public <R extends Number> SDFrame<T> mapPercent(Function<T, R> get, SetFunction<T, BigDecimal> set) {
        return mapPercent(get,set,2);
    }

    @Override
    public <R extends Number> SDFrame<T> mapPercent(Function<T,R> get, SetFunction<T,BigDecimal> set, int scale){
        toLists().forEach(e -> {
            R value = get.apply(e);
            BigDecimal percentageValue = MathUtils.percentage(MathUtils.toBigDecimal(value), scale);
            set.accept(e,percentageValue);
        });
        return this;
    }

    @Override
    public SDFrame<List<T>> partition(int n) {
        return returnDF(new PartitionList<>(toLists(), n));
    }


    @Override
    public SDFrame<T> append(T t) {
        List<T> ts = toLists();
        ts.add(t);
        data = ts.stream();
        return this;
    }

    @Override
    public SDFrame<T> union(IFrame<T> other) {
        if (other.count() <= 0){
            return this;
        }
        List<T> ts = toLists();
        ts.addAll(other.toLists());
        return returnDF(ts);
    }

    @Override
    public <R, K> SDFrame<R> join(IFrame<K> other, JoinOn<T, K> on, Join<T, K, R> join) {
        return returnDF(joinList(other,on,join));
    }

    @Override
    public <R, K> SDFrame<R> join(IFrame<K> other, JoinOn<T, K> on) {
        return join(other,on,new DefaultJoin<>());
    }

    @Override
    public <R, K> SDFrame<R> leftJoin(IFrame<K> other, JoinOn<T, K> on, Join<T, K, R> join) {
        return returnDF(leftJoinList(other,on,join));
    }

    @Override
    public <R, K> SDFrame<R> leftJoin(IFrame<K> other, JoinOn<T, K> on) {
        return leftJoin(other,on,new DefaultJoin<>());
    }

    @Override
    public <R, K> SDFrame<R> rightJoin(IFrame<K> other, JoinOn<T, K> on, Join<T, K, R> join) {
        return returnDF(rightJoinList(other,on,join));
    }

    @Override
    public <R, K> SDFrame<R> rightJoin(IFrame<K> other, JoinOn<T, K> on) {
        return rightJoin(other,on,new DefaultJoin<>());
    }

    @Override
    public SDFrame<FI2<T, Integer>> addSortNoCol() {
        List<FI2<T, Integer>> result = new ArrayList<>();
        int index = 1;
        for (T t : this) {
            result.add(new FI2<>(t,index++));
        }
        return returnDF(result);
    }

    @Override
    public SDFrame<FI2<T, Integer>> addSortNoCol(Comparator<T> comparator) {
        return sortAsc(comparator).addSortNoCol();
    }

    @Override
    public <R extends Comparable<R>> SDFrame<FI2<T, Integer>> addSortNoCol(Function<T, R> function) {
        return addSortNoCol(Comparator.comparing(function));
    }


    @Override
    public SDFrame<T> addSortNoCol(SetFunction<T, Integer> set) {
        int index = 0;
        for (T t : this) {
           set.accept(t,index++);
        }
        return this;
    }

    @Override
    public SDFrame<FI2<T, Integer>> addRankingSameCol(Comparator<T> comparator) {
        return returnDF(rankingSameAsc(toLists(),comparator));
    }

    @Override
    public <R extends Comparable<R>> SDFrame<FI2<T, Integer>> addRankingSameColAsc(Function<T, R> function) {
        return addRankingSameCol(Comparator.comparing(function));
    }

    @Override
    public SDFrame<T> addRankingSameCol(Comparator<T> comparator, SetFunction<T, Integer> set) {
        List<FI2<T, Integer>> tmpList = rankingSameAsc(toLists(), comparator);
        for (FI2<T, Integer> p : tmpList) {
            set.accept(p.getC1(),p.getC2());
        }
        return this;
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> addRankingSameColAsc(Function<T, R> function, SetFunction<T, Integer> set) {
        return addRankingSameCol(Comparator.comparing(function),set);
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> addRankingSameColDesc(Function<T, R> function, SetFunction<T, Integer> set) {
        return addRankingSameCol(Comparator.comparing(function).reversed(),set);
    }

    @Override
    public List<T> toLists() {
        List<T> tmp = data.collect(toList());
        data = tmp.stream();
        return tmp;
    }

    @Override
    public  Stream<T> stream(){
        return data;
    }

    @Override
    public long count() {
        List<T> tmp = stream().collect(toList());
        data = tmp.stream();
        return tmp.size();
    }

    @Override
    public <K> SDFrame<FI2<K, List<T>>> group(Function<T, K> key) {
        return returnDF(groupKey(key));
    }


    /**
     * ===========================   排序相关  =====================================
     **/

    @Override
    public SDFrame<T> sortDesc(Comparator<T> comparator) {
        data = stream().sorted(comparator.reversed());
        return this;
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> sortDesc(Function<T, R> function) {
        sortDesc(Comparator.comparing(function));
        return this;
    }

    @Override
    public SDFrame<T> sortAsc(Comparator<T> comparator) {
        data = stream().sorted(comparator);
        return this;
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> sortAsc(Function<T, R> function) {
        sortAsc(Comparator.comparing(function));
        return this;
    }

    @Override
    public SDFrame<T> cutRankingSameAsc(Comparator<T> comparator, int n) {
        List<FI2<T, Integer>> tmpList = rankingSameAsc(toLists(), comparator, n);
        return returnThis(tmpList.stream().map(FI2::getC1).collect(toList()).stream());
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> cutRankingSameAsc(Function<T, R> function, int n) {
        return this.cutRankingSameAsc(Comparator.comparing(function),n);
    }

    @Override
    public SDFrame<T> cutRankingSameDesc(Comparator<T> comparator, int n) {
        return this.cutRankingSameAsc(comparator.reversed(),n);
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> cutRankingSameDesc(Function<T, R> function, int n) {
        return this.cutRankingSameDesc(Comparator.comparing(function),n);
    }


    /** ===========================   截取相关  ===================================== **/

    @Override
    public SDFrame<T> cutFirst(int n) {
        DFList<T> first = new DFList<>(toLists()).first(n);
        List<T> build = first.build();
        return returnThis(build);
    }

    @Override
    public SDFrame<T> cutLast(int n) {
        DFList<T> first = new DFList<>(toLists()).last(n);
        data = first.build().stream();
        return this;
    }

    @Override
    public SDFrame<T> distinct() {
        data = stream().distinct();
        return this;
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> distinct(Function<T, R> function) {
        return distinct(Comparator.comparing(function));
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> distinct(Comparator<T> comparator) {
        ArrayList<T> tmp = stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparator)), ArrayList::new));
        data = tmp.stream();
        return this;
    }

    @Override
    public long countDistinct(Comparator<T> comparator) {
        return distinct(comparator).count();
    }

    @Override
    public <R extends Comparable<R>> long countDistinct(Function<T, R> function) {
        return countDistinct(Comparator.comparing(function));
    }

    /**
     * ===========================   筛选相关  =====================================
     **/
    @Override
    public SDFrame<T> where(Predicate<? super T> predicate) {
        return returnThis(stream().filter(predicate));
    }

    @Override
    public <R> SDFrame<T> whereNull(Function<T, R> function) {
        return returnThis(whereNotNullStream(function));
    }

    public <R> SDFrame<T> whereNotNull(Function<T, R> function) {
        return returnThis(whereNotNullStream(function));
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> whereBetween(Function<T, R> function, R start, R end) {
        if (start == null && end == null) {
            return this;
        }
        return returnThis(whereBetweenStream(function,start,end));
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> whereBetweenN(Function<T, R> function, R start, R end) {
        if (start == null && end == null) {
            return this;
        }
        return returnThis(whereBetweenNStream(function,start,end));
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> whereBetweenR(Function<T, R> function, R start, R end) {
        // 筛选条件都不存在默认不筛选
        if (start == null && end == null) {
            return this;
        }
        return returnThis(whereBetweenRStream(function,start,end));
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> whereBetweenL(Function<T, R> function, R start, R end) {
        if (start == null && end == null) {
            return this;
        }
        return returnThis(whereBetweenLStream(function,start,end));
    }

    @Override
    public <R extends Comparable<R>> SDFrame<T> whereNotBetween(Function<T, R> function, R start, R end) {
        if (start == null || end == null) {
            return this;
        }
        return returnThis(whereNotBetweenStream(function,start,end));
    }
    @Override
    public <R extends Comparable<R>> SDFrame<T> whereNotBetweenN(Function<T, R> function, R start, R end) {
        if (start == null || end == null) {
            return this;
        }
        return returnThis(whereNotBetweenNStream(function,start,end));
    }


    public <R> SDFrame<T> whereIn(Function<T, R> function, List<R> list) {
        if (list == null || list.isEmpty()) {
            return this;
        }
        return returnThis(whereInStream(function,list));
    }


    public <R> SDFrame<T> whereNotIn(Function<T, R> function, List<R> list) {
        if (list == null || list.isEmpty()) {
            return this;
        }
        return returnThis(whereNotInStream(function,list));
    }


    public SDFrame<T> whereTrue(Predicate<T> predicate) {
        return returnThis(stream().filter(predicate));
    }


    public SDFrame<T> whereNotTrue(Predicate<T> predicate) {
        return whereTrue(predicate.negate());
    }


    public <R> SDFrame<T> whereEq(Function<T, R> function, R value) {
        if (null == value) {
            return this;
        }
        return  returnThis(whereEqStream(function,value));
    }


    public <R> SDFrame<T> whereNotEq(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereNotEqStream(function,value));
    }


    public <R extends Comparable<R>> SDFrame<T> whereGt(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereGtStream(function,value));
    }


    public <R extends Comparable<R>> SDFrame<T> whereGe(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereGeStream(function,value));
    }


    public <R extends Comparable<R>> SDFrame<T> whereLt(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereLtStream(function,value));
    }


    public <R extends Comparable<R>> SDFrame<T> whereLe(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereLeStream(function,value));
    }


    public <R> SDFrame<T> whereLike(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereLikeStream(function,value));
    }


    public <R> SDFrame<T> whereNotLike(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereNotLikeStream(function,value));
    }


    public <R> SDFrame<T> whereLikeLeft(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereLikeLeftStream(function,value));
    }


    public <R> SDFrame<T> whereLikeRight(Function<T, R> function, R value) {
        if (value == null) {
            return this;
        }
        return returnThis(whereLikeRightStream(function,value));
    }



    /** ===========================   分组相关  ===================================== **/



    public <K,R extends Number> SDFrame<FI2<K, BigDecimal>> groupBySum(Function<T, K> key, NumberFunction<T,R> value) {
        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.summingBigDecimalForNumber(value);
        List<FI2<K, BigDecimal>> collect = groupKey(key, tBigDecimalCollector);
        return returnDF(collect);
    }


    public <K, J,R extends Number> SDFrame<FI3<K, J, BigDecimal>> groupBySum(Function<T, K> key,
                                                            Function<T, J> key2,
                                                            NumberFunction<T,R> value) {
        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.summingBigDecimalForNumber(value);
        List<FI3<K, J, BigDecimal>> collect = groupKey(key, key2, tBigDecimalCollector);
        return returnDF(collect);
    }



    public <K, J, H,R extends Number> SDFrame<FI4<K, J, H, BigDecimal>> groupBySum(Function<T, K> key,
                                                                  Function<T, J> key2,
                                                                  Function<T, H> key3,
                                                                  NumberFunction<T,R> value) {
        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.summingBigDecimalForNumber(value);
        List<FI4<K, J, H, BigDecimal>> collect = groupKey(key, key2, key3, tBigDecimalCollector);
        return returnDF(collect);
    }


    public <K> SDFrame<FI2<K, Long>> groupByCount(Function<T, K> key) {
        Collector<Object, ?, Long> counting = counting();
        Map<K, Long> collect = stream().collect(groupingBy(key, counting));
        return returnDF(FrameUtil.toListFI2(collect));
    }


    public <K, J> SDFrame<FI3<K, J, Long>> groupByCount(Function<T, K> key,
                                                        Function<T, J> key2) {
        Collector<Object, ?, Long> counting = counting();
        Map<K, Map<J, Long>> collect = stream().collect(groupingBy(key, groupingBy(key2, counting)));
        return returnDF(FrameUtil.toListFI3(collect));
    }


    public <K, J, H> SDFrame<FI4<K, J, H, Long>> groupByCount(Function<T, K> key,
                                                              Function<T, J> key2,
                                                              Function<T, H> key3) {
        Collector<Object, ?, Long> counting = counting();
        Map<K, Map<J, Map<H, Long>>> collect = stream().collect(groupingBy(key, groupingBy(key2, groupingBy(key3, counting))));
        return returnDF(FrameUtil.toListFI4(collect));
    }


    public <K,R extends Number> SDFrame<FI3<K, BigDecimal,Long>> groupBySumCount(Function<T, K> key, NumberFunction<T,R> value) {
        List<T> dataList = toLists();
        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.summingBigDecimalForNumber(value);
        List<FI2<K, BigDecimal>> sumList = returnDF(dataList).groupKey(key, tBigDecimalCollector);
        List<FI2<K, Long>> countList =  returnDF(dataList).groupByCount(key).toLists();
        Map<K, Long> countMap = countList.stream().collect(toMap(FI2::getC1, FI2::getC2));
        List<FI3<K, BigDecimal, Long>> collect = sumList.stream().map(e -> new FI3<>(e.getC1(), e.getC2(), countMap.get(e.getC1()))).collect(Collectors.toList());
        return returnDF(collect);
    }


    public <K, J,R extends Number> SDFrame<FI4<K, J, BigDecimal, Long>> groupBySumCount(Function<T, K> key,
                                                                       Function<T, J> key2,
                                                                       NumberFunction<T,R> value) {
        List<T> dataList = toLists();
        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.summingBigDecimalForNumber(value);
        List<FI3<K, J, BigDecimal>> sumList = returnDF(dataList).groupKey(key, key2, tBigDecimalCollector);
        List<FI3<K, J, Long>> countList =  returnDF(dataList).groupByCount(key, key2).toLists();
        // 合并sum和count字段
        Map<String, FI3<K, J, Long>> countMap = countList.stream().collect(toMap(e -> e.getC1() + "_" + e.getC2(), Function.identity()));
        List<FI4<K, J, BigDecimal, Long>> collect = sumList.stream().map(e -> {
            FI3<K, J, Long> countItem = countMap.get(e.getC1() + "_" + e.getC2());
            return new FI4<>(e.getC1(), e.getC2(), e.getC3(), countItem.getC3());
        }).collect(Collectors.toList());
        return returnDF(collect);
    }


    public <K,R extends Number> SDFrame<FI2<K, BigDecimal>> groupByAvg(Function<T, K> key,
                                                      NumberFunction<T,R> value) {
        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.averagingBigDecimal(value, 2, BigDecimal.ROUND_HALF_UP);
        List<FI2<K, BigDecimal>> collect = groupKey(key, tBigDecimalCollector);
        return returnDF(collect);
    }


    public <K, J,R extends Number> SDFrame<FI3<K, J, BigDecimal>> groupByAvg(Function<T, K> key,
                                                            Function<T, J> key2,
                                                            NumberFunction<T,R> value) {

        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.averagingBigDecimal(value, 2, BigDecimal.ROUND_HALF_UP);
        List<FI3<K, J, BigDecimal>> collect = groupKey(key, key2, tBigDecimalCollector);
        return returnDF(collect);
    }


    public <K, J, H,R extends Number> SDFrame<FI4<K, J, H, BigDecimal>> groupByAvg(Function<T, K> key,
                                                                  Function<T, J> key2,
                                                                  Function<T, H> key3,
                                                                  NumberFunction<T,R> value) {
        Collector<T, ?, BigDecimal> tBigDecimalCollector = CollectorsPlusUtil.averagingBigDecimal(value, 2, BigDecimal.ROUND_HALF_UP);
        List<FI4<K, J, H, BigDecimal>> collect = groupKey(key, key2, key3, tBigDecimalCollector);
        return returnDF(collect);
    }


    @Override
    public <K, V extends Comparable<? super V>> SDFrame<FI2<K, T>> groupByMax(Function<T, K> key,
                                                                      Function<T, V> value) {
        Map<K, T> collect = stream().collect(groupingBy(key, collectingAndThen(toList(), getListMaxFunction(value))));
        return returnDF(FrameUtil.toListFI2(collect));
    }

    @Override
    public <K, J, V extends Comparable<? super V>> SDFrame<FI3<K, J, T>> groupByMax(Function<T, K> key, Function<T, J> key2, Function<T, V> value) {
        Map<K, Map<J, T>> collect = groupToMap(key, key2,getListMaxFunction(value));
        return returnDF(FrameUtil.toListFI3(collect));
    }

    @Override
    public <K, V extends Comparable<? super V>> SDFrame<FI2<K, V>> groupByMaxValue(Function<T, K> key, Function<T, V> value) {
        return groupByMax(key, value).map(e -> new FI2<>(e.getC1(), getApplyValue(value,e.getC2())));
    }

    @Override
    public <K, J, V extends Comparable<? super V>> SDFrame<FI3<K, J, V>> groupByMaxValue(Function<T, K> key, Function<T, J> key2, Function<T, V> value) {
        return groupByMax(key, key2,value).map(e -> new FI3<>(e.getC1(),e.getC2(),getApplyValue(value,e.getC3())));
    }

    @Override
    public <K, V extends Comparable<? super V>> SDFrame<FI2<K, T>> groupByMin(Function<T, K> key,
                                                                      Function<T, V> value) {
        Map<K, T> collect = stream().collect(groupingBy(key, collectingAndThen(toList(), e -> e.stream().min(Comparator.comparing(value)).orElse(null))));
        return returnDF(FrameUtil.toListFI2(collect));
    }

    @Override
    public <K, J, V extends Comparable<? super V>> SDFrame<FI3<K, J, T>> groupByMin(Function<T, K> key, Function<T, J> key2, Function<T, V> value) {
        Map<K, Map<J, T>> collect = groupToMap(key, key2,getListMinFunction(value));
        return returnDF(FrameUtil.toListFI3(collect));
    }

    @Override
    public <K, V extends Comparable<? super V>> SDFrame<FI2<K, V>> groupByMinValue(Function<T, K> key, Function<T, V> value) {
        return groupByMin(key, value).map(e -> new FI2<>(e.getC1(),getApplyValue(value,e.getC2())));
    }

    @Override
    public <K, J, V extends Comparable<? super V>> SDFrame<FI3<K, J, V>> groupByMinValue(Function<T, K> key, Function<T, J> key2, Function<T, V> value) {
        return groupByMin(key, key2,value).map(e -> new FI3<>(e.getC1(),e.getC2(),getApplyValue(value,e.getC3())));
    }

    @Override
    public <K, V extends Comparable<? super V>> SDFrame<FI2<K, MaxMin<V>>> groupByMaxMinValue(Function<T, K> key,
                                                                                      Function<T, V> value) {
        Map<K, MaxMin<V>> map = stream().collect(groupingBy(key, collectingAndThen(toList(), getListGroupMaxMinValueFunction(value))));
        return returnDF(FrameUtil.toListFI2(map));
    }

    @Override
    public <K, J, V extends Comparable<? super V>> SDFrame<FI3<K, J, MaxMin<V>>> groupByMaxMinValue(Function<T, K> key,
                                                                                            Function<T, J> key2,
                                                                                            Function<T, V> value) {
        Map<K, Map<J, MaxMin<V>>> map = stream().collect(groupingBy(key, groupingBy(key2, collectingAndThen(toList(), getListGroupMaxMinValueFunction(value)))));
        return returnDF(FrameUtil.toListFI3(map));
    }

    @Override
    public <K, V extends Comparable<? super V>> SDFrame<FI2<K, MaxMin<T>>> groupByMaxMin(Function<T, K> key,
                                                                                 Function<T, V> value) {
        Map<K, MaxMin<T>> map = stream().collect(groupingBy(key, collectingAndThen(toList(), getListGroupMaxMinFunction(value))));
        return returnDF(FrameUtil.toListFI2(map));
    }

    @Override
    public <K, J, V extends Comparable<? super V>> SDFrame<FI3<K, J, MaxMin<T>>> groupByMaxMin(Function<T, K> key,
                                                                                       Function<T, J> key2,
                                                                                       Function<T, V> value) {
        Map<K, Map<J, MaxMin<T>>> map = stream().collect(groupingBy(key, groupingBy(key2, collectingAndThen(toList(), getListGroupMaxMinFunction(value)))));
        return returnDF(FrameUtil.toListFI3(map));
    }

    @Override
    public  SDFrame<FI2<T, Integer>> overRowNumber(OverParam<T> overParam) {
        SupplierFunction<T,Integer> supplier = new SupplierFunction<T,Integer>() {
            @Override
            public List<FI2<T, Integer>> get(List<T> windowList) {
                List<FI2<T, Integer>> result = new ArrayList<>();
                int index = 1;
                for (T t : windowList) {
                    result.add(new FI2<>(t,index++));
                }
                return result;
            }
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public  SDFrame<FI2<T, Integer>> overRank(OverParam<T> overParam) {
        SupplierFunction<T,Integer> supplier = (windowList) -> {
            Comparator<T> comparator = overParam.getComparator();
            List<FI2<T, Integer>> result = new ArrayList<>();
            int n = windowList.size();
            int rank = 1;
            result.add(new FI2<>(windowList.get(0), 1));
            for (int i = 1; i < windowList.size(); i++) {
                T pre = windowList.get(i-1);
                T cur = windowList.get(i);
                if (comparator.compare(pre,cur) != 0){
                    rank = i + 1;
                }
                if (rank <= n){
                    result.add(new FI2<>(cur, rank));
                }else {
                    break;
                }
            }
            return result;
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public  SDFrame<FI2<T, Integer>> overDenseRank(OverParam<T> overParam) {
        SupplierFunction<T,Integer> supplier = (windowList) -> {
            List<FI2<T, Integer>> result = new ArrayList<>();
            int n = windowList.size();
            int rank = 1;
            result.add(new FI2<>(windowList.get(0), 1));
            for (int i = 1; i < windowList.size(); i++) {
                T pre = windowList.get(i-1);
                T cur = windowList.get(i);
                if (overParam.getComparator().compare(pre,cur) != 0){
                    rank += 1;
                }
                if (rank <= n){
                    result.add(new FI2<>(cur, rank));
                }else {
                    break;
                }
            }
            return result;
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public  SDFrame<FI2<T, BigDecimal>> overPercentRank(OverParam<T> overParam) {
        SupplierFunction<T,BigDecimal> supplier = (windowList) -> {
            // (rank-1) / (rows-1)
            List<FI2<T, BigDecimal>> result = new ArrayList<>();
            int n = windowList.size();
            int rank = 1;
            result.add(new FI2<>(windowList.get(0), new BigDecimal("0.00")));
            for (int i = 1; i < windowList.size(); i++) {
                T pre = windowList.get(i-1);
                T cur = windowList.get(i);
                if (overParam.getComparator().compare(pre,cur) != 0){
                    rank = i + 1;
                }
                if (rank <= n){
                    BigDecimal divide = MathUtils.divide((rank - 1), windowList.size() - 1, 2);
                    result.add(new FI2<>(cur, divide));
                }else {
                    break;
                }
            }
            return result;
        };
        return returnDF(overAbject(overParam,supplier));

    }

    @Override
    public  SDFrame<FI2<T, BigDecimal>> overCumeDist(OverParam<T> overParam) {
        SupplierFunction<T,BigDecimal> supplier = (windowList) -> {
            List<FI2<T, Integer>> result = new ArrayList<>();
            int n = windowList.size();
            int rank = 1;
            Map<Integer,Integer> rankCountMap = new HashMap<>();
            for (int i = 1; i < windowList.size(); i++) {
                T pre = windowList.get(i-1);
                T cur = windowList.get(i);
                if (overParam.getComparator().compare(pre,cur) != 0){
                    // 次数的rank累积的计数最大
                    rankCountMap.put(rank,i);
                    rank = i + 1;
                }
                if (rank <= n){
                    result.add(new FI2<>(cur, rank));
                }else {
                    break;
                }
            }
            // 最大排名
            rankCountMap.computeIfAbsent(rank, k -> windowList.size());
            List<FI2<T, BigDecimal>> resultList = new ArrayList<>();
            result.forEach(e -> {
                Integer count = rankCountMap.get(e.getC2());
                BigDecimal divide = MathUtils.divide(count, windowList.size(), 2);
                resultList.add(new FI2<>(e.getC1(),divide));
            });
            return resultList;
        };

        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <F> SDFrame<FI2<T, F>> overLag(OverParam<T> overParam, Function<T, F> field, int n) {
        SupplierFunction<T,F> supplier = (windowList) -> {
            List<FI2<T, F>> result = new ArrayList<>();
            for (int i = 0; i < windowList.size(); i++) {
                int preIndex = i - n;
                F value = null;
                if (preIndex >= 0 && preIndex < windowList.size()){
                    value = field.apply(windowList.get(preIndex));
                }
                result.add(new FI2<>(windowList.get(i),value));
            }
            return result;
        };

        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <F> SDFrame<FI2<T, F>> overLead(OverParam<T> overParam, Function<T, F> field, int n) {
        SupplierFunction<T,F> supplier = (windowList) -> {
            List<FI2<T, F>> result = new ArrayList<>();
            for (int i = 0; i < windowList.size(); i++) {
                int afterIndex = i + n;
                F value = null;
                if (afterIndex >= 0 && afterIndex < windowList.size()){
                    value = field.apply(windowList.get(afterIndex));
                }
                result.add(new FI2<>(windowList.get(i),value));
            }
            return result;
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <F> SDFrame<FI2<T, F>> overNthValue(OverParam<T> overParam, Function<T, F> field, int n) {
        SupplierFunction<T,F> supplier = (windowList) -> {
            int index;
            if (n == -1){
                index = windowList.size() - 1;
            }else {
                index = n - 1;
            }
            if (index >= 0 && index < windowList.size()){
                F value = field.apply( windowList.get(index));
                return windowList.stream().map(e -> new FI2<>(e, value)).collect(toList());
            }else {
                return windowList.stream().map(e -> new FI2<T,F>(e,null)).collect(toList());
            }
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <F> SDFrame<FI2<T, F>> overFirstValue(OverParam<T> overParam, Function<T, F> field, int n) {
        return overNthValue(overParam,field,1);
    }

    @Override
    public <F> SDFrame<FI2<T, F>> overLastValue(OverParam<T> overParam, Function<T, F> field, int n) {
        return overNthValue(overParam,field,-1);
    }

    @Override
    public <F> SDFrame<FI2<T, BigDecimal>> overSum(OverParam<T> overParam, Function<T, F> field) {
        SupplierFunction<T,BigDecimal> supplier = (windowList) -> {
            BigDecimal value = SDFrame.read(windowList).sum(field);
            return windowList.stream().map(e -> new FI2<>(e,value)).collect(toList());
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <F> SDFrame<FI2<T, BigDecimal>> overAvg(OverParam<T> overParam, Function<T, F> field) {
        SupplierFunction<T,BigDecimal> supplier = (windowList) -> {
            BigDecimal value = SDFrame.read(windowList).avg(field);
            return windowList.stream().map(e -> new FI2<>(e,value)).collect(toList());
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <F extends Comparable<? super F>>  SDFrame<FI2<T, F>> overMaxValue(OverParam<T> overParam, Function<T, F> field) {
        SupplierFunction<T,F> supplier = (windowList) -> {
            F value = SDFrame.read(windowList).maxValue(field);
            return windowList.stream().map(e -> new FI2<>(e,value)).collect(toList());
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <F extends Comparable<? super F>> SDFrame<FI2<T, F>> overMinValue(OverParam<T> overParam, Function<T, F> field) {
        SupplierFunction<T,F> supplier = (windowList) -> {
            F value = SDFrame.read(windowList).minValue(field);
            return windowList.stream().map(e -> new FI2<>(e,value)).collect(toList());
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public SDFrame<FI2<T, Integer>> overCount(OverParam<T> overParam) {
        SupplierFunction<T,Integer> supplier = (windowList) -> {
            int count = windowList.size();
            return windowList.stream().map(e -> new FI2<>(e,count)).collect(toList());
        };
        return returnDF(overAbject(overParam,supplier));
    }

    @Override
    public <G, C> SDFrame<T> replenish(Function<T, G> groupDim, Function<T, C> collectDim, List<C> allDim, ReplenishFunction<G, C, T> getEmptyObject) {
        return returnDF(replenish(toLists(),groupDim,collectDim,allDim,getEmptyObject));
    }

    @Override
    public <C> SDFrame<T> replenish(Function<T, C> collectDim, List<C> allDim, Function<C, T> getEmptyObject) {
        return returnDF(replenish(toLists(),collectDim,allDim,getEmptyObject));
    }

    @Override
    public <G, C> SDFrame<T> replenish(Function<T, G> groupDim, Function<T, C> collectDim, ReplenishFunction<G, C, T> getEmptyObject) {
        return returnDF(replenish(toLists(),groupDim,collectDim,getEmptyObject));
    }

    protected SDFrame<T> returnThis(Stream<T> stream) {
        this.data = stream;
        return this;
    }

    protected SDFrame<T> returnThis(List<T> dataList) {
        this.data = dataList.stream();
        return this;
    }

    protected <R> SDFrameImpl<R> returnDF(List<R> dataList) {
        return new SDFrameImpl<>(dataList);
    }
    
    
}
