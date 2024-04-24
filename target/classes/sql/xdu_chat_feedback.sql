-- MySQL dump 10.13  Distrib 8.0.28, for Win64 (x86_64)
--
-- Host: 211.159.166.139    Database: xdu_chat
-- ------------------------------------------------------
-- Server version	5.6.50-log

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
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '反馈记录id',
  `user_id` char(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户id，从统一身份认证平台获取',
  `type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '反馈类型，1-like点赞，2-dislike点踩，3-feedback反馈',
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
  `record` longtext COLLATE utf8mb4_unicode_ci COMMENT '反馈所对应的记录信息',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT '反馈内容，如果是赞/踩则空，反馈则有内容',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除，"0"未删除，"1"删除',
  PRIMARY KEY (`id`),
  KEY `feedback__index_time` (`time`) COMMENT 'time时间索引',
  KEY `feedback__index_user` (`user_id`) COMMENT 'userid列的索引',
  KEY `feedback_index_type` (`type`) COMMENT 'type类型索引'
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feedback`
--

LOCK TABLES `feedback` WRITE;
/*!40000 ALTER TABLE `feedback` DISABLE KEYS */;
INSERT INTO `feedback` VALUES (4,'345235543',1,'2024-04-24 05:47:05','',NULL,0),(5,'342342',1,'2024-04-24 06:53:45',NULL,NULL,0),(6,'342342',1,'2024-04-24 06:54:59','sdfsafd asdfs',NULL,0),(7,'1',1,'2024-04-24 07:04:25','1',NULL,0);
/*!40000 ALTER TABLE `feedback` ENABLE KEYS */;
UNLOCK TABLES;
