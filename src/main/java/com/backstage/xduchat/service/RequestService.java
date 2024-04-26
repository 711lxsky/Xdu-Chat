package com.backstage.xduchat.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RequestService {

    private final Map<String, ReentrantLock> requestLocks = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Sinks.Empty<Void>>> requestCompletionSignals = new ConcurrentHashMap<>();

    public Flux<String> processRequest(String userId, String recordId, String jsonMessages) {
        String identifier = userId + recordId;
        
        // 为每个唯一的请求标识符确保有一个锁和一个完成信号
        requestLocks.computeIfAbsent(identifier, key -> new ReentrantLock());
        requestCompletionSignals.computeIfAbsent(identifier, key -> new AtomicReference<>(Sinks.empty()));

        ReentrantLock lock = requestLocks.get(identifier);
        AtomicReference<Sinks.Empty<Void>> completionSignalRef = requestCompletionSignals.get(identifier);

        return Mono.<String>create(sink -> {
            boolean acquired = lock.tryLock();
            try {
                if (!acquired) {
                    // 如果锁无法立即获得，说明有另一个请求正在处理
                    // 订阅前一个请求的完成信号，一旦收到信号，立即返回错误
                    completionSignalRef.get().asMono().subscribe(
                        nullValue -> sink.error(new HttpException("请求正在处理中")),
                        sink::error,
                        sink::success
                    );
                } else {
                    // 成功获得锁，表示没有其他并发请求
                    try {
                        // 这里执行实际的请求处理逻辑
                        internalProxy(userId, recordId, jsonMessages).subscribe(
                            result -> {
                                // 处理成功，向下游发送成功信号
                                sink.success();
                            },
                            error -> {
                                // 处理失败，向下游发送错误信号
                                sink.error(error);
                            }
                        );
                    } finally {
                        // 设置新的完成信号，供后续的请求订阅
                        completionSignalRef.set(Sinks.empty());
                        // 释放锁之前，发出当前请求已完成的信号
                        completionSignalRef.get().tryEmitEmpty();
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                sink.error(e);
                if (acquired) {
                    lock.unlock();
                }
            }
        }).flux().doFinally(signalType -> {
            // 清理资源，避免内存泄露
            requestLocks.remove(identifier);
            requestCompletionSignals.remove(identifier);
        });
    }

    private Mono<String> internalProxy(String userId, String recordId, String jsonMessages) {
        // 实现省略，返回异步处理结果
        return Mono.just("Result");
    }

    public static class HttpException extends RuntimeException {
        public HttpException(String message) {
            super(message);
        }
    }
}
