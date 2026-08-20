package com.jason.ai.knowledgebase.service.sse;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.config.ChatProperties;

/** 提供准入阶段显式预留槽位的单实例全局 FIFO 队列。 */
@Component
public class ChatGenerationQueue {

    private final BlockingQueue<GenerationTask> queue;
    private final Semaphore slots;

    public ChatGenerationQueue(ChatProperties properties) {
        int capacity = properties.getSse().getMaxQueuedRequests();
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.slots = new Semaphore(capacity);
    }

    /**
     * 尝试为准入请求预留队列槽位。
     *
     * @return 预留成功时返回 true
     */
    public boolean reserveSlot() {
        return slots.tryAcquire();
    }

    /** 释放尚未用于入队的预留槽位。 */
    public void releaseReservation() {
        slots.release();
    }

    /**
     * 使用已经预留的槽位提交任务。
     *
     * @param task 生成任务
     * @throws IllegalStateException 预留状态与实际队列容量不一致时抛出
     */
    public void submitReserved(GenerationTask task) {
        if (!queue.offer(task)) {
            slots.release();
            throw new IllegalStateException("预留的 FIFO 槽位不可用");
        }
    }

    /**
     * 阻塞取得下一个生成任务，并同步归还队列槽位。
     *
     * @return 下一个生成任务
     * @throws InterruptedException 等待期间线程被中断时抛出
     */
    public GenerationTask take() throws InterruptedException {
        GenerationTask task = queue.take();
        slots.release();
        return task;
    }

    /**
     * 取消尚未出队的任务并归还槽位。
     *
     * @param task 待取消任务
     * @return 成功移除时返回 true
     */
    public boolean cancel(GenerationTask task) {
        if (task != null && queue.remove(task)) {
            slots.release();
            return true;
        }
        return false;
    }
}