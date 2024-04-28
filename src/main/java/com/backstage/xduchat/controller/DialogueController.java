package com.backstage.xduchat.controller;

import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.DialogueDTO;
import com.backstage.xduchat.service.DialogueService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: 711lxsky
 */
@Api(tags = "对话记录")
@RestController
public class DialogueController {

    private final DialogueService dialogueService;

    public DialogueController(DialogueService dialogueService) {
        this.dialogueService = dialogueService;
    }

    @ApiOperation(value = "添加对话记录")
    @PostMapping(path = "/add-record")
    public Result<?> addDialogue(@RequestBody DialogueDTO dialogueDTO){
        return dialogueService.addDialogue(dialogueDTO);
    }

    @ApiOperation(value = "获取某个用户所有对话记录")
    @GetMapping(path = "/get-record")
    public Result<?> getAllDialogueForUser(@RequestParam(name = "uid") String uid){
        return dialogueService.getAllDialogueForUser(uid);
    }

    @ApiOperation(value = "删除某条对话记录")
    @GetMapping(path = "/delete-record")
    public Result<?> deleteDialogue(@RequestParam(name = "id") String id,
                                    @RequestParam(name = "uid") String uid){
        return dialogueService.deleteDialogue(id, uid);
    }

}
