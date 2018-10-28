package walker.protocol.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Pagination<T> {

    /**
     * 当前页码
     */
    private int currentPage = 1;

    /**
     * 每页大小
     */
    private int pageSize = 10;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 总行数
     */
    private long rows;

    /**
     * 数据
     */
    private List<T> data;

    /**
     * constructor
     *
     * @param rows 总行数
     * @param data 数据
     */
    public Pagination(long rows, List<T> data) {
        this.rows = 0 >= rows ? 0 : rows;
        this.pages = (rows + this.pageSize - 1) / this.pageSize;
        this.data = data;
    }

    /**
     * constructor
     *
     * @param currentPage 当前页码
     * @param pageSize    每页大小
     * @param rows        总行数
     * @param data        数据
     */
    public Pagination(int currentPage, int pageSize, long rows, List<T> data) {
        this.currentPage = 0 >= currentPage ? 1 : currentPage;
        this.pageSize = 0 >= pageSize ? 10 : pageSize;
        this.rows = 0 >= rows ? 0 : rows;
        this.pages = (rows + this.pageSize - 1) / this.pageSize;
        this.data = data;
    }

}