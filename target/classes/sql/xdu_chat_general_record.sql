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
-- Table structure for table `general_record`
--

DROP TABLE IF EXISTS `general_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `general_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `user_id` char(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户id，从统一身份认证平台获取',
  `time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录保存时间',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT '记录内容',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志，"0"未删除，"1"删除',
  PRIMARY KEY (`id`),
  KEY `general_record__index_time` (`time`) COMMENT 'time列索引',
  KEY `general_record__index_user` (`user_id`) COMMENT '用户id列索引'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通记录表，存放走代理进行对话的所有消息，且只插入，不更新';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `general_record`
--

LOCK TABLES `general_record` WRITE;
/*!40000 ALTER TABLE `general_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `general_record` ENABLE KEYS */;
UNLOCK TABLES;
