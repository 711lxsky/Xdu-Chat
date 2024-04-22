-- MySQL dump 10.13  Distrib 8.2.0, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: xdu_chat
-- ------------------------------------------------------
-- Server version	8.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `feedback`
--

DROP TABLE IF EXISTS `feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈记录id',
  `user_id` char(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户id，从统一身份认证平台获取',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '反馈类型，1-like点赞，2-dislike点踩，3-feedback反馈',
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT '反馈内容，如果是赞/踩则空，反馈则有内容',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除，"0"未删除，"1"删除',
  PRIMARY KEY (`id`),
  KEY `feedback__index_time` (`time`) COMMENT 'time时间索引',
  KEY `feedback__index_user` (`user_id`) COMMENT 'userid列的索引',
  KEY `feedback_index_type` (`type`) COMMENT 'type类型索引'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `general_record`
--

DROP TABLE IF EXISTS `general_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `general_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `user_id` char(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户id，从统一身份认证平台获取',
  `time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录保存时间',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT '记录内容',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志，"0"未删除，"1"删除',
  PRIMARY KEY (`id`),
  KEY `general_record__index_time` (`time`) COMMENT 'time列索引',
  KEY `general_record__index_user` (`user_id`) COMMENT '用户id列索引'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通记录表，存放走代理进行对话的所有消息，且只插入，不更新';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-04-22 13:24:58
