# -*- coding: utf-8 -*-
import os
import re
from pathlib import Path

# 日志目录
log_dir = Path(r'F:\desktop\iflowtest\DOC\DevLogs_架构与开发日志')

# 获取所有.md文件（排除合并后的文件和报告文件）
md_files = [f for f in log_dir.glob('*.md') 
            if '合并' not in f.name 
            and '汇总' not in f.name
            and '对比报告' not in f.name]

# 按日期排序（从文件名提取日期）
def get_date(filename):
    match = re.match(r'(\d{4}-\d{2}-\d{2})', filename)
    if match:
        return match.group(1)
    return '0000-00-00'  # 无日期的放最后

# 排序：有日期的按日期排序，无日期的按名称排序
dated_files = []
undated_files = []

for f in md_files:
    if get_date(f.name) != '0000-00-00':
        dated_files.append(f)
    else:
        undated_files.append(f)

dated_files.sort(key=lambda x: get_date(x.name))
undated_files.sort(key=lambda x: x.name)

sorted_files = dated_files + undated_files

# 合并内容
output = []
output.append('# FocusFlow 开发日志汇总')
output.append('')
output.append('> 本文档由所有开发日志合并而成，每份日志作为一个章节')
output.append(f'> 合并时间：2026-04-11')
output.append(f'> 日志总数：{len(sorted_files)}')
output.append('')
output.append('---')
output.append('')

# 目录
output.append('## 目录')
output.append('')
chapter_num = 1
for f in sorted_files:
    # 提取标题（去掉日期前缀）
    title = f.stem
    title = re.sub(r'^\d{4}-\d{2}-\d{2}_', '', title)
    title = re.sub(r'^\d{4}-\d{2}-\d{2}_', '', title)  # 处理可能的重复
    output.append(f'{chapter_num}. {title}')
    chapter_num += 1
output.append('')
output.append('---')
output.append('')

# 合并每个文件
chapter_num = 1
for f in sorted_files:
    # 提取标题
    title = f.stem
    date = get_date(f.name)
    title_clean = re.sub(r'^\d{4}-\d{2}-\d{2}_', '', title)
    
    # 添加章节标题
    if date != '0000-00-00':
        output.append(f'## 第{chapter_num}章 {title_clean}')
        output.append(f'> 日期：{date}')
    else:
        output.append(f'## 第{chapter_num}章 {title_clean}')
    output.append('')
    
    # 读取文件内容
    try:
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            # 去掉文件开头的标题（如果有）
            lines = content.split('\n')
            # 跳过开头的#标题行
            start_idx = 0
            for i, line in enumerate(lines):
                if line.strip() and not line.startswith('#'):
                    start_idx = i
                    break
                elif line.startswith('#') and i == 0:
                    start_idx = 1
                    continue
            
            body = '\n'.join(lines[start_idx:])
            output.append(body)
    except Exception as e:
        output.append(f'*读取失败：{e}*')
    
    output.append('')
    output.append('---')
    output.append('')
    
    chapter_num += 1

# 写入合并后的文件
output_path = log_dir / 'FocusFlow开发日志汇总.md'
with open(output_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(output))

print(f'合并完成：{output_path}')
print(f'共合并 {len(sorted_files)} 个日志文件')