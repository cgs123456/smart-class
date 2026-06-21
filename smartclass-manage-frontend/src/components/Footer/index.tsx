import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import '@umijs/max';
import React from 'react';

const Footer: React.FC = () => {
  const defaultMessage = '程序猿-cgs';
  const currentYear = new Date().getFullYear();
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      copyright={`${currentYear} ${defaultMessage}`}
      links={[
        {
          key: 'cgs的博客小屋',
          title: 'cgs的博客小屋',
          href: 'https://www.blog.cgs.cn',
          blankTarget: true,
        },
        {
          key: 'github',
          title: (
            <>
              <GithubOutlined /> cgs的Github
            </>
          ),
          href: 'https://github.com/cgs',
          blankTarget: true,
        },
      ]}
    />
  );
};
export default Footer;
